package fr.upmc.ppm.amiralex.activities;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

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
import fr.upmc.ppm.amiralex.views.FileItemModeler;
import fr.upmc.ppm.amiralex.views.FileItemSorter;

public class FileExplorerActivity extends ListActivity {

	/* ---------------------------------------------------------------------- */
	// membres
	/* ---------------------------------------------------------------------- */

	protected File current, root; // dossiers courant et racine
	protected TextView whereTv; // textView affichant l'emplacement courant
	protected ImageButton rootBtn, parentBtn, refreshBtn; // boutons de
															// navigation
	private File copy; // fichier dont la copie est demandée
	private boolean cut = false; // copie ou coupie xD
	private File trash; // emplacement de la corbeille
	private Map<String, String> trashFiles = new HashMap<String, String>();
	private FileArrayAdapter listAdapter; // adapter de la listView

	// mode d'affichage
	private FileItemModeler mod = FileItemModeler.LIST;
	// mode de tri
	private FileItemSorter sort = FileItemSorter.BY_DONT_CARE;

	/* ---------------------------------------------------------------------- */
	// constantes
	/* ---------------------------------------------------------------------- */

	private static final String TRASH_NAME = "RECYCLE.BIN";
	public static final String PREF_NAME = "File_Explorer_Pref";
	private static final String PREF_KEY_CURRENT = "currentFile";
	public static final String PREF_KEY_MOD = "typeMod";
	public static final String PREF_KEY_SORT = "sortList";
	private static final String PREF_KEY_RESTORE = "restore.";

	/* ---------------------------------------------------------------------- */
	// évènements système
	/* ---------------------------------------------------------------------- */

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.main);

		String rootName = (getIntent().getExtras() != null && (rootName = getIntent()
				.getExtras().getString("file")) != null) ? rootName
				: "/mnt/sdcard/";

		root = new File(rootName);

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
	 * Lorsque l'activity reprend la main, on rafraichi les données : par
	 * exemple dans le cas ou la corbeille est passée par là...
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 10) loadPreferences();
		refreshDirectory();
	}

	/**
	 * On indexe les options sur leur code de string dans le registre. Cela nous
	 * permet d'identifier le menu séléctionné lors du onOptionsItemSelected
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
		menu.add(Menu.NONE, R.string.options_settings, Menu.NONE,
				R.string.options_settings).setIcon(R.drawable.settings_selector);

		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * On indexe les menus sur leur code de string dans le registre. Cela nous
	 * permet d'identifier le menu séléctionné lors du onContextItemSelected
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
			// on ne peut pas coller si rien n'est copié
			if ((copy == null && R.string.menu_paste != i) || (copy != null))
				menu.add(Menu.NONE, i, Menu.NONE, i);

		super.onCreateContextMenu(menu, v, menuInfo);
	}

	/**
	 * On récupère l'élément selectionné dans le ListAdapter et on affiche son
	 * détail
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
		case R.string.options_settings:
			openPreferences();
			break;
		}

		return super.onOptionsItemSelected(item);
	}

	// petit problème avec la touche retour et la preview d'une musique : ça
	// remontait dans l'arbo
	private boolean bugPreviewMusic = false;

	/**
	 * On remonte dans l'arborescence jusqu'au noeud root. Si on arrive au noeud
	 * root, l'application est fermée
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
	// méthodes & utilitaires
	/* ---------------------------------------------------------------------- */

	/**
	 * On affiche la liste des fils du dossier selectionné, dans la listview
	 * 
	 * @param f
	 *            dossier selectionné
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

		// on réactualise la liste des éléments
		listAdapter = new FileArrayAdapter(this, R.layout.filerow, files, mod);
		this.setListAdapter(listAdapter);
		listAdapter.sort(sort);

		return true;

	}

	/**
	 * Racourci d'écriture pour le rafraichissement des éléments du dossier
	 * courant, on réactualise par la même le nombre d'éléments des dossiers et
	 * la taille des fichiers (au niveau des infos affichées)
	 */
	private void refreshDirectory() {
		setDirectory(current);
	}

	/**
	 * Ouvre le panneau de détails du fichier selectionné
	 * 
	 * @param f
	 *            fichier à détailler
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
	 * Balance un Intent à la corbeille Et paf, ça fait des chocapics !
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
	 * Vide la corbeille par récursion
	 * 
	 * @return
	 */
	public boolean clearTrash() {
		if (!hasTrash())
			return false;
		boolean b = removePermanentlyFileWithLoading(trash);
		trashFiles.clear();
		createTrash();
		return b;
	}

	/**
	 * Créé la corbeille
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
	 * Vérifie si la corbeille existe ou non
	 * 
	 * @return
	 */
	public boolean hasTrash() {
		return trash != null && trash.exists();
	}

	/**
	 * Vérifie si l'activity courante Est la corbeille On aurait pu utiliser un
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
		mod = FileItemModeler.values()[item];
		((FileArrayAdapter) getListAdapter()).setMod(mod);
		getListView().invalidateViews();
	}

	/**
	 * Modifie le type de tri
	 * 
	 * @param item
	 */
	protected void switchSort(int item) {
		sort = FileItemSorter.values()[item];
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
	
	private void openPreferences() {
		startActivityForResult(new Intent(this, FileSettingsActivity.class), 10);
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
	 * Création d'un nouveau dossier Il sera placé dans le dossier courant
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
		if (f.renameTo(new File(new EnhancedFile(current).getRealAbsolutePath() + "/" + name)))
			refreshDirectory();
		else
			Toast.makeText(FileExplorerActivity.this,
					R.string.rename_file_error, Toast.LENGTH_LONG);
	}

	/**
	 * Chargement des préférences les préférences de la corbeille et celles du
	 * panneau principal sont dissociées
	 */
	private void loadPreferences() {

		SharedPreferences settings = getSharedPreferences(
				PREF_NAME + isTrash(), Context.MODE_WORLD_WRITEABLE);

		mod = FileItemModeler.values()[settings.getInt(PREF_KEY_MOD,
				FileItemModeler.LIST.ordinal())];
		if (current == null)
			current = new File(settings.getString(PREF_KEY_CURRENT,
					root.getAbsolutePath()));
		sort = FileItemSorter.values()[settings.getInt(PREF_KEY_SORT, 0)];
		
		Log.d("blah", mod + " " + sort);
		
		/*
		Map<String, ?> prefs = settings.getAll();
		for (Entry<String, ?> pref : prefs.entrySet())
			if (pref.getKey().startsWith(PREF_KEY_RESTORE))
				trashFiles.put(
						pref.getKey().substring(PREF_KEY_RESTORE.length()),
						(String) pref.getValue());*/

	}

	/**
	 * Enregistrement des préférences : pérénisation des données les préférences
	 * de la corbeille et celles du panneau principal sont dissociées
	 */
	private void commitPreferences() {
		
		SharedPreferences settings = getSharedPreferences(
				PREF_NAME + isTrash(), Context.MODE_WORLD_WRITEABLE);
		SharedPreferences.Editor editor = settings.edit();

		editor.putInt(PREF_KEY_MOD, mod.ordinal());
		editor.putInt(PREF_KEY_SORT, sort.ordinal());
		editor.putString(PREF_KEY_CURRENT, current.getAbsolutePath());
		/*for (Entry<String, String> pref : trashFiles.entrySet())
			editor.putString(PREF_KEY_RESTORE + pref.getKey(), pref.getValue());*/

		editor.commit();

	}

	/**
	 * Création d'un nouveau dossier à partir du nom transmis depuis une boite
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
	 * Renommage d'un élement à partir du nom transmis depuis une boite de
	 * dialogue
	 * 
	 * @param f
	 */
	public void renameFileWithAlertDialog(final File f) {

		View v = getLayoutInflater().inflate(R.layout.input, null);
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
	// méthodes qui devront être rendues asynchrones à therme
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

		// le fichier est déplacé dans la corbeille, non plus supprimé
		// trashFiles.put(PREF_KEY_RESTORE + f.getAbsolutePath(), f.getName());
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
