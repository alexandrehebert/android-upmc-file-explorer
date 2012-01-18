package fr.upmc.ppm.amiralex.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import fr.upmc.ppm.amiralex.R;
import fr.upmc.ppm.amiralex.tools.EnhancedFile;
import fr.upmc.ppm.amiralex.tools.FileTypeResolver.FileType;
import fr.upmc.ppm.amiralex.tools.Utils;

public class FileDetailsActivity extends Activity {

	private EnhancedFile file;
	private FileType type;
	private String ext, size;
	private TextView nameView, sizeView, typeView;
	private ImageView iconView;
	private String emptyString, elementsString;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.details);

		emptyString = getResources().getString(R.string.file_more_empty);
		elementsString = getResources().getString(R.string.file_more_elements);

		file = new EnhancedFile(getIntent().getExtras().getString("file"));
		type = file.getType();
		ext = file.getExt()
				+ (type != null ? " (" + type.getType(this) + ")" : "");

		long length = file.getLength();
		size = !file.isDirectory() ? Utils.formatSize(length, 0)
				: (length == 0 ? emptyString : length + " " + elementsString);

		nameView = (TextView) findViewById(R.id.detailsName);
		iconView = (ImageView) findViewById(R.id.detailsIcon);

		sizeView = (TextView) findViewById(R.id.detailsSize);
		typeView = (TextView) findViewById(R.id.detailsType);

		nameView.setText(file.getName());
		typeView.setText(ext);
		sizeView.setText(size);
		iconView.setImageResource(file.getImageRessource());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (type.isPreviewable())
			menu.add(Menu.NONE, R.string.options_preview, Menu.NONE, R.string.options_preview).setIcon(R.drawable.preview_selector);
		if (!file.isDirectory())
			menu.add(Menu.NONE, R.string.options_remove, Menu.NONE, R.string.options_remove).setIcon(R.drawable.remove_selector);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.string.options_preview:
			startActivity(type.toIntent(file));
			break;
		case R.string.options_remove:
			if(file.delete())
				finish();
			else
				Toast.makeText(this, R.string.remove_file_error, Toast.LENGTH_SHORT);
			break;
		}
		return super.onOptionsItemSelected(item);
	}

}
