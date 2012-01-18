package fr.upmc.ppm.amiralex.activities;

import java.io.File;

import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import fr.upmc.ppm.amiralex.R;

public class FileBinActivity extends FileExplorerActivity {
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		((TextView) findViewById(R.id.title)).setText(R.string.options_bin);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		boolean b = super.onCreateOptionsMenu(menu);

		menu.removeItem(R.string.options_bin);
		menu.add(Menu.NONE, R.string.options_emptybin, Menu.NONE, R.string.options_emptybin)
			.setIcon(R.drawable.bin_selector);
		
		return b;
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		return;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return false;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		
		switch(item.getItemId()) {
		case R.string.options_emptybin:
			createFolderWithAlertDialog();
			break;
		}
		
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	public boolean setDirectory(File f) {
		boolean b = super.setDirectory(f);
		parentBtn.setEnabled(b && !current.getAbsolutePath().equals(root.getAbsolutePath()));
		return b;
	}
	
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK)
			if (current.getAbsolutePath().equals(root.getAbsolutePath())) {
				finish();
				return false;
			}
		return super.onKeyUp(keyCode, event);
	}
	
}
