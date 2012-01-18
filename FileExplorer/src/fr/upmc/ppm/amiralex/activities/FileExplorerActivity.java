package fr.upmc.ppm.amiralex.activities;

import java.io.File;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import fr.upmc.ppm.amiralex.R;
import fr.upmc.ppm.amiralex.tools.EnhancedFile;
import fr.upmc.ppm.amiralex.tools.FileTypeResolver.FileType;
import fr.upmc.ppm.amiralex.views.FileArrayAdapter;

public class FileExplorerActivity extends ListActivity {

	protected File current, root;
	protected TextView whereTv;
	protected ImageButton rootBtn, parentBtn, refreshBtn;
	private File copy;
	private boolean cut = false;

	@Override
	protected void onCreate(Bundle b) {
		super.onCreate(b);
		setContentView(R.layout.main);
		
		String rootName = (getIntent().getExtras() != null 
				&& (rootName = getIntent().getExtras().getString("file")) != null)
			? rootName
			: "/mnt/sdcard/";
		
		root = new File(rootName);
		current = root;
		if (!root.exists()) root.mkdirs(); // root = new File("/");
		
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
				setDirectory(current);
			}
		});
		
		setDirectory(root);
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		// rafraichissement
		setDirectory(current);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		menu.add(Menu.NONE, R.string.options_share, Menu.NONE, R.string.options_share)
			.setIcon(R.drawable.share_selector);
		menu.add(Menu.NONE, R.string.options_create, Menu.NONE, R.string.options_create)
			.setIcon(R.drawable.create_selector);
		menu.add(Menu.NONE, R.string.options_bin, Menu.NONE, R.string.options_bin)
			.setIcon(R.drawable.bin_selector);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	/**
	 * On indexe les menus sur leur code de change dans le registre
	 * Cela nous permet d'identifier le menu séléctionné lors du onContextItemSelected
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		
		//int[] menus = new int[] {R.string.menu_remove, R.string.menu_move, R.string.menu_copy, R.string.menu_rename, R.string.menu_details};
		int[] menus = new int[] {R.string.menu_copy, R.string.menu_cut, R.string.menu_paste, 
				R.string.menu_rename, R.string.menu_remove, R.string.menu_details};
		for (int i : menus)
			// on ne peut pas coller si rien n'est copié
			if ((copy == null && R.string.menu_paste != i) || (copy != null))
				menu.add(Menu.NONE, i, Menu.NONE, i);
		
		super.onCreateContextMenu(menu, v, menuInfo);
	}
	
	/**
	 * On récupère l'élément selectionné dans le ListAdapter et on affiche son détail
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		
		// AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
		File f = (File) ((FileArrayAdapter)getListAdapter()).getCurrent();
		switch(item.getItemId()) {
		case R.string.menu_details:
			setFile(f);
			break;
		case R.string.menu_remove:
			removeFileWithLoading(f);
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
			if (copy == null) break;
			if (cut) cutFileWithLoading(copy, f);
			else if (!cut) copyFileWithLoading(copy, f);
			copy = null;
			break;
		}
		
		setDirectory(current);
		return super.onContextItemSelected(item);
	}

	/**
	 * Choix d'un item dans le menu d'options
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()) {
		case R.string.options_create:
			createFolderWithAlertDialog();
			break;
		case R.string.options_bin:
			startActivity(new Intent(this, FileBinActivity.class).putExtra("file", root.getAbsolutePath() + "/RECYCLE.BIN"));
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	// petit problème avec la touche retour et la preview d'une musique : ça remontait dans l'arbo
	private boolean bugPreviewMusic = false;
	/**
	 * On remonte dans l'arborescence jusqu'au noeud root
	 * Si on arrive au noeud root, l'application est fermée
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
	 * @param f dossier selectionné
	 * @return
	 */
	public boolean setDirectory(File f) {
		
		if (f == null || !f.isDirectory())
			return false;

		this.setProgressBarIndeterminateVisibility(true);
		
		whereTv.setText(f.getAbsolutePath());

		File[] files = (files = f.listFiles()) != null ? files : new File[] {};
		if (!f.getAbsolutePath().equals(root.getAbsolutePath()) && files.length == 0)
			return setFile(f);
		
		current = f;
		
		ArrayAdapter<File> adapter = new FileArrayAdapter(this,
				R.layout.filerow, files);
		this.setListAdapter(adapter);
		
		this.setProgressBarIndeterminateVisibility(false);
		
		return true;
		
	}
	
	/**
	 * Ouvre le panneau de détails du fichier selectionné
	 * 
	 * @param f fichier à détailler
	 * @return
	 */
	public boolean setFile(File f) {
		
		//if (f == null || f.isDirectory())
		//	return false;
		if  (f.getAbsolutePath().equals(root.getAbsolutePath()))
			return true;
		
		Intent intent;
		intent = new Intent(this, FileDetailsActivity.class);
		intent.putExtra("file", f.getAbsolutePath());
		
		startActivityForResult(intent, 0);
		
		return true;
		
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
		}
		else
			return setFile(f);
		
	}
	
	/**
	 * Création d'un nouveau dossier
	 * Il sera placé dans le dossier courant
	 * 
	 * @param f
	 */
	public void createFolder(File f) {
		Log.d("debug", f.getAbsolutePath());
		if (f.mkdirs())
			setDirectory(f);
		else
			Toast.makeText(FileExplorerActivity.this, R.string.create_dir_error, Toast.LENGTH_LONG).show();
	}
	
	public void renameFile(File f, String name) {
		if (f.renameTo(new File(current.getAbsolutePath() + "/" + name)))
    		setDirectory(current);
    	else
    		Toast.makeText(FileExplorerActivity.this, R.string.rename_file_error, Toast.LENGTH_LONG);
	}
	
	/**
	 * Création d'un nouveau dossier à partir du nom transmis depuis une boite de dialogue
	 */
	public void createFolderWithAlertDialog() {
		
		final EditText edit = new EditText(this);
		new AlertDialog.Builder(this)
		    .setTitle(R.string.options_create)
		    .setView(edit)
		    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		        	createFolder(new File(current.getAbsoluteFile() + "/" + edit.getText() + "/"));
		        }
		    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {}
		    }).show();
		
	}
	
	/**
	 * Renommage d'un élement à partir du nom transmis depuis une boite de dialogue
	 * 
	 * @param f
	 */
	public void renameFileWithAlertDialog(final File f) {
		
		final EditText edit = new EditText(this);
		edit.setText(f.getName());
		new AlertDialog.Builder(this)
		    .setTitle(R.string.menu_rename)
		    .setView(edit)
		    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {
		        	renameFile(f, edit.getText().toString());
		        }
		    }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
		        public void onClick(DialogInterface dialog, int whichButton) {}
		    }).show();
		
	}
	
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
		if (!f.isDirectory()) {
			if (f.delete()) return true;
			Toast.makeText(this, R.string.remove_file_error, Toast.LENGTH_SHORT);
			return false;
		}
		else {
			if (new EnhancedFile(f).deleteCascade()) return true;
			Toast.makeText(this, R.string.remove_folder_error, Toast.LENGTH_SHORT);
			return false;
		}
	}

}
