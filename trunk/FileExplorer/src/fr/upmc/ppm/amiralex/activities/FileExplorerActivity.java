package fr.upmc.ppm.amiralex.activities;

import java.io.File;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.ClipboardManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import fr.upmc.ppm.amiralex.R;
import fr.upmc.ppm.amiralex.tools.EnhancedFile;
import fr.upmc.ppm.amiralex.tools.FileTypeResolver.FileType;
import fr.upmc.ppm.amiralex.views.FileArrayAdapter;
import fr.upmc.ppm.amiralex.views.FileArrayComparator;
import fr.upmc.ppm.amiralex.views.TypeMod;

public class FileExplorerActivity extends ListActivity {

	/* ---------------------------------------------------------------------- */
	// membres
	/* ---------------------------------------------------------------------- */

	protected File current, root; // dossiers courant et racine
	protected TextView whereTv; // textView affichant l'emplacement courant
	protected ImageButton rootBtn, parentBtn, refreshBtn; // boutons de
															// navigation
	private File copy; // fichier dont la copie est demand�e
	private boolean cut = false; // copie ou coupie xD
	private File trash; // emplacement de la corbeille
	private FileArrayAdapter listAdapter; // adapter de la listView
	private TypeMod mod = TypeMod.LIST; // mode d'affichage
	private FileArrayComparator sort = FileArrayComparator.BY_DONT_CARE; // mode
																			// de
																			// tri

	/* ---------------------------------------------------------------------- */
	// constantes
	/* ---------------------------------------------------------------------- */

	private static final String TRASH_NAME = "RECYCLE.BIN";
	private static final String PREF_NAME = "File_Explorer_Pref";
	private static final String PREF_KEY_CURRENT = "currentFile";
	private static final String PREF_KEY_MOD = "typeMod";
	private static final String PREF_KEY_SORT = "sortList";

	/* ---------------------------------------------------------------------- */
	// �v�nements syst�me
	/* ---------------------------------------------------------------------- */

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.main);

		String rootName = (getIntent().getExtras() != null && (rootName = getIntent()
				.getExtras().getString("file")) != null) ? rootName
				: "/mnt/sdcard/";

		root = new File(rootName);
		current = root;

		if (!root.exists())
			root.mkdirs(); // root = new File("/");
		if (!isTrash())
			createTrash();
		else
			trash = root;

		whereTv = (TextView) findViewById(R.id.currentFileTextView);
		rootBtn = (ImageButton) findViewById(R.id.rootButton);
		parentBtn = (ImageButton) findViewById(R.id.parentButton);
		refreshBtn = (ImageButton) findViewById(R.id.refreshButton);

		rootBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setDirectory(root);
			}
		});
		parentBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setDirectory(current.getParentFile());
			}
		});

		refreshBtn.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				refreshDirectory();
			}
		});

		whereTv.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				clipboard.setText(whereTv.getText());
				Toast.makeText(
						FileExplorerActivity.this,
						"\"" + whereTv.getText() + "\" "
								+ getResources().getString(R.string.copy_shot),
						Toast.LENGTH_SHORT).show();
			}
		});

		loadPreferences();
		refreshDirectory();
	}

	@Override
	protected void onStop() {
		super.onStop();
		commitPreferences();
	}

	/**
	 * Lorsque l'activity reprend la main, on rafraichi les donn�es : par
	 * exemple dans le cas ou la corbeille est pass�e par l�...
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		refreshDirectory();
	}

	/**
	 * On indexe les options sur leur code de string dans le registre. Cela nous
	 * permet d'identifier le menu s�l�ctionn� lors du onOptionsItemSelected
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		menu.add(Menu.NONE, R.string.options_create, Menu.NONE,
				R.string.options_create).setIcon(R.drawable.create_selector);
		menu.add(Menu.NONE, R.string.options_bin, Menu.NONE,
				R.string.options_bin).setIcon(R.drawable.bin_selector);
		menu.add(Menu.NONE, R.string.options_mod, Menu.NONE,
				R.string.options_mod).setIcon(R.drawable.mod_selector);
		menu.add(Menu.NONE, R.string.options_sort, Menu.NONE,
				R.string.options_sort).setIcon(R.drawable.sort_selector);

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * On indexe les menus sur leur code de string dans le registre. Cela nous
	 * permet d'identifier le menu s�l�ctionn� lors du onContextItemSelected
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {

		// int[] menus = new int[] {R.string.menu_remove, R.string.menu_move,
		// R.string.menu_copy, R.string.menu_rename, R.string.menu_details};
		int[] menus = new int[] { R.string.menu_copy, R.string.menu_cut,
				R.string.menu_paste, R.string.menu_rename,
				R.string.menu_remove, R.string.menu_details };
		for (int i : menus)
			// on ne peut pas coller si rien n'est copi�
			if ((copy == null && R.string.menu_paste != i) || (copy != null))
				menu.add(Menu.NONE, i, Menu.NONE, i);

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	/**
	 * On r�cup�re l'�l�ment selectionn� dans le ListAdapter et on affiche son
	 * d�tail
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {

		// AdapterView.AdapterContextMenuInfo info =
		// (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		File f = (File) ((FileArrayAdapter) getListAdapter()).getCurrent();
		switch (item.getItemId()) {
		case R.string.menu_details:
			setFile(f);
			break;
		case R.string.menu_remove:
			removeFileWithLoading(f);
			break;
		case R.string.menu_cut:
			cut = true;
			copy = f;
			break;
		case R.string.menu_rename:
			renameFileWithAlertDialog(f);
			break;
		case R.string.menu_copy:
			cut = false;
			copy = f;
			break;
		case R.string.menu_paste:
			if (copy == null)
				break;
			if (cut)
				cutFileWithLoading(copy, f);
			else if (!cut)
				copyFileWithLoading(copy, f);
			copy = null;
			break;
		}

		refreshDirectory();
		return super.onContextItemSelected(item);
	}

	/**
	 * Choix d'un item dans le menu d'options
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.string.options_create:
			createFolderWithAlertDialog();
			break;
		case R.string.options_bin:
			openTrash();
			break;
		case R.string.options_sort:
			openSortMenu();
			break;
		case R.string.options_mod:
			openModMenu();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	// petit probl�me avec la touche retour et la preview d'une musique : �a
	// remontait dans l'arbo
	private boolean bugPreviewMusic = false;

	/**
	 * On remonte dans l'arborescence jusqu'au noeud root. Si on arrive au noeud
	 * root, l'application est ferm�e
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {

		if (!bugPreviewMusic)
			if (keyCode == KeyEvent.KEYCODE_BACK)
				if (setDirectory(current.getParentFile()))
					return true;

		bugPreviewMusic = false;

		return super.onKeyUp(keyCode, event);
	}

	/* ---------------------------------------------------------------------- */
	// m�thodes & utilitaires
	/* ---------------------------------------------------------------------- */

	/**
	 * On affiche la liste des fils du dossier selectionn�, dans la listview
	 * 
	 * @param f
	 *            dossier selectionn�
	 * @return
	 */
	public boolean setDirectory(File f) {

		if (f == null || !f.isDirectory())
			return false;

		// si le dossier que l'on veut afficher est la corbeille
		if (!isTrash() && f.getAbsolutePath().equals(trash.getAbsolutePath()))
			return openTrash();

		whereTv.setText(f.getAbsolutePath());

		File[] files = (files = f.listFiles()) != null ? files : new File[] {};
		// si le File est un fichier, on l'ouvre par Intent
		if (!f.getAbsolutePath().equals(root.getAbsolutePath())
				&& files.length == 0)
			return setFile(f);

		current = f;

		// on r�actualise la liste des �l�ments
		listAdapter = new FileArrayAdapter(this, R.layout.filerow, files, mod);
		this.setListAdapter(listAdapter);
		listAdapter.sort(sort);

		return true;

	}

	/**
	 * Racourci d'�criture pour le rafraichissement des �l�ments du dossier
	 * courant, on r�actualise par la m�me le nombre d'�l�ments des dossiers et
	 * la taille des fichiers (au niveau des infos affich�es)
	 */
	private void refreshDirectory() {
		setDirectory(current);
	}

	/**
	 * Ouvre le panneau de d�tails du fichier selectionn�
	 * 
	 * @param f
	 *            fichier � d�tailler
	 * @return
	 */
	public boolean setFile(File f) {

		// if (f == null || f.isDirectory())
		// return false;
		if (f.getAbsolutePath().equals(root.getAbsolutePath()))
			return true;

		Intent intent;
		intent = new Intent(this, FileDetailsActivity.class);
		intent.putExtra("file", f.getAbsolutePath());

		startActivityForResult(intent, 0);

		return true;

	}

	/**
	 * Balance un Intent � la corbeille Et paf, �a fait des chocapics !
	 * 
	 * @return
	 */
	public boolean openTrash() {
		if (isTrash() || !hasTrash())
			return false;
		startActivityForResult(
				new Intent(this, FileBinActivity.class).putExtra("file",
						trash.getAbsolutePath()).putExtra("i_am_a_trash", true),
				1);
		return true;
	}

	/**
	 * Vide la corbeille par r�cursion
	 * 
	 * @return
	 */
	public boolean clearTrash() {
		if (!hasTrash())
			return false;
		boolean b = removePermanentlyFileWithLoading(trash);
		createTrash();
		return b;
	}

	/**
	 * Cr�� la corbeille
	 * 
	 * @return
	 */
	public void createTrash() {
		if (hasTrash())
			return;
		trash = new File(root.getAbsoluteFile()
				+ (isTrash() ? "" : "/" + TRASH_NAME));
		trash.mkdirs();
	}

	/**
	 * V�rifie si la corbeille existe ou non
	 * 
	 * @return
	 */
	public boolean hasTrash() {
		return trash != null && trash.exists();
	}

	/**
	 * V�rifie si l'activity courante Est la corbeille On aurait pu utiliser un
	 * instanceOf ou autres, mais faut bien rigoler un peu
	 * 
	 * @return
	 */
	public boolean isTrash() {
		return getIntent().hasExtra("i_am_a_trash");
	}

	/**
	 * Modifie le type d'affichage
	 * 
	 * @param item
	 */
	private void switchMod(int item) {
		mod = TypeMod.values()[item];
		((FileArrayAdapter) getListAdapter()).setMod(mod);
		getListView().invalidateViews();
	}

	/**
	 * Modifie le type de tri
	 * 
	 * @param item
	 */
	protected void switchSort(int item) {
		sort = FileArrayComparator.values()[item];
		if (listAdapter != null)
			listAdapter.sort(sort);
	}

	/**
	 * Dialogue de selection du mode de tri
	 */
	private void openSortMenu() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.sort_by_title);
		builder.setItems(R.array.sort_by,
				new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int item) {
						switchSort(item);
					}
				});
		builder.create().show();
	}

	/**
	 * Dialogue de selection du mode d'affichage
	 */
	private void openModMenu() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle(R.string.mod_by_title);
		builder.setItems(R.array.mod_by, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int item) {
				switchMod(item);
			}
		});
		builder.create().show();
	}

	/**
	 * On ouvre une preview du fichier si possible, en fonction du type de media
	 * 
	 * @param f
	 * @return
	 */
	public boolean setPreviewFile(File f) {

		EnhancedFile ff = new EnhancedFile(f);
		FileType ft = ff.getType();
		Intent intent;

		if (ft.isPreviewable()) {
			intent = ft.toIntent(f);
			startActivityForResult(intent, 0);
			bugPreviewMusic = (ft.getTypeRessource() == R.string.type_audio);
			return true;
		} else
			return setFile(f);

	}

	/**
	 * Cr�ation d'un nouveau dossier Il sera plac� dans le dossier courant
	 * 
	 * @param f
	 */
	public void createFolder(File f) {
		Log.d("debug", f.getAbsolutePath());
		if (f.mkdirs())
			setDirectory(f);
		else
			Toast.makeText(FileExplorerActivity.this,
					R.string.create_dir_error, Toast.LENGTH_LONG).show();
	}

	/**
	 * Renomme le dossier/fichier
	 * 
	 * @param f
	 * @param name
	 */
	public void renameFile(File f, String name) {
		if (f.renameTo(new File(current.getAbsolutePath() + "/" + name)))
			refreshDirectory();
		else
			Toast.makeText(FileExplorerActivity.this,
					R.string.rename_file_error, Toast.LENGTH_LONG);
	}

	/**
	 * Chargement des pr�f�rences
	 */
	private void loadPreferences() {

		SharedPreferences settings = getSharedPreferences(
				PREF_NAME + isTrash(), 0);
		mod = TypeMod.values()[settings.getInt(PREF_KEY_MOD,
				TypeMod.LIST.ordinal())];
		current = new File(settings.getString(PREF_KEY_CURRENT,
				root.getAbsolutePath()));
		sort = FileArrayComparator.values()[settings.getInt(PREF_KEY_SORT, 0)];

	}

	/**
	 * Enregistrement des pr�f�rences : p�r�nisation des donn�es
	 */
	private void commitPreferences() {

		SharedPreferences settings = getSharedPreferences(
				PREF_NAME + isTrash(), 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putInt(PREF_KEY_MOD, mod.ordinal());
		editor.putInt(PREF_KEY_SORT, sort.ordinal());
		editor.putString(PREF_KEY_CURRENT, current.getAbsolutePath());
		editor.commit();

	}

	/**
	 * Cr�ation d'un nouveau dossier � partir du nom transmis depuis une boite
	 * de dialogue
	 */
	public void createFolderWithAlertDialog() {

		View v = getLayoutInflater().inflate(R.layout.input, null);
		final EditText edit = (EditText) v.findViewById(R.id.input);
		new AlertDialog.Builder(this)
				.setTitle(R.string.options_create)
				.setView(v)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								createFolder(new File(current.getAbsoluteFile()
										+ "/" + edit.getText() + "/"));
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						}).show();

	}

	/**
	 * Renommage d'un �lement � partir du nom transmis depuis une boite de
	 * dialogue
	 * 
	 * @param f
	 */
	public void renameFileWithAlertDialog(final File f) {

		View v = getLayoutInflater()
				.inflate(R.layout.input, this.getListView());
		final EditText edit = (EditText) v.findViewById(R.id.input);
		edit.setText(f.getName());
		new AlertDialog.Builder(this)
				.setTitle(R.string.menu_rename)
				.setView(v)
				.setPositiveButton(R.string.ok,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
								renameFile(f, edit.getText().toString());
							}
						})
				.setNegativeButton(R.string.cancel,
						new DialogInterface.OnClickListener() {
							public void onClick(DialogInterface dialog,
									int whichButton) {
							}
						}).show();

	}

	/* ---------------------------------------------------------------------- */
	// m�thodes qui devront �tre rendues asynchrones � therme
	/* ---------------------------------------------------------------------- */
	
	public boolean copyFileWithLoading(File from, File to) {
		if (new EnhancedFile(from).copyTo(to))
			return true;
		Toast.makeText(this, R.string.copy_file_error, Toast.LENGTH_SHORT);
		return false;
	}

	public boolean cutFileWithLoading(File from, File to) {
		if (new EnhancedFile(from).moveTo(to))
			return true;
		Toast.makeText(this, R.string.cut_file_error, Toast.LENGTH_SHORT);
		return false;
	}

	private boolean removeFileWithLoading(File f) {
		/*
		 * if (!f.isDirectory()) { if (f.delete()) return true;
		 * Toast.makeText(this, R.string.remove_file_error, Toast.LENGTH_SHORT);
		 * return false; } else { if (new EnhancedFile(f).deleteCascade())
		 * return true; Toast.makeText(this, R.string.remove_folder_error,
		 * Toast.LENGTH_SHORT); return false; }
		 */
		
		// le fichier est d�plac� dans la corbeille, non plus supprim�
		return cutFileWithLoading(f, trash);
	}

	private boolean removePermanentlyFileWithLoading(File f) {
		if (!f.isDirectory()) {
			if (f.delete())
				return true;
			Toast.makeText(this, R.string.remove_file_error, Toast.LENGTH_SHORT);
			return false;
		} else {
			if (new EnhancedFile(f).deleteCascade())
				return true;
			Toast.makeText(this, R.string.remove_folder_error,
					Toast.LENGTH_SHORT);
			return false;
		}
		/*
		 * TEST d'IMPLEMENTATION de L'ASYNCHRONE + logo de chargement
		 * 
		 * // this.setProgressBarIndeterminateVisibility(false); AsyncTask<File,
		 * Integer, Exception> at = new AsyncTask<File, Integer, Exception>() {
		 * 
		 * @Override protected void onPreExecute() { super.onPreExecute(); }
		 * 
		 * @Override protected Exception doInBackground(File... f) { for (int i
		 * = 0; i < f.length; i++) { f[i].delete(); this.publishProgress(i,
		 * f.length); } return null; }
		 * 
		 * @Override protected void onProgressUpdate(Integer... values) {
		 * super.onProgressUpdate(values); }
		 * 
		 * @Override protected void onPostExecute(Exception result) {
		 * super.onPostExecute(result); if (result != null)
		 * Toast.makeText(FileExplorerActivity.this, result.getMessage(),
		 * Toast.LENGTH_SHORT); } }; at.execute(new
		 * EnhancedFile(f).listAllFiles().toArray(new File[]{})); return true;
		 */
	}

}
