package fr.upmc.ppm.amiralex.views;

import java.io.File;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import fr.upmc.ppm.amiralex.R;
import fr.upmc.ppm.amiralex.activities.FileExplorerActivity;
import fr.upmc.ppm.amiralex.tools.EnhancedFile;
import fr.upmc.ppm.amiralex.tools.Utils;

/**
 * 
 * @author alexandre hebert
 * 
 */
public class FileArrayAdapter extends ArrayAdapter<File> {

	public static String emptyString, elementsString;
	private File current;
	private FileItemModeler mod;

	public FileArrayAdapter(Context context, int textViewResourceId,
			File[] objects, FileItemModeler mod) {
		super(context, textViewResourceId, objects);
		if (emptyString == null) ressources(context);
		this.mod = mod;
	}
	
	public static final void ressources(Context context) {
		emptyString = context.getResources()
				.getString(R.string.file_more_empty);
		elementsString = context.getResources().getString(
				R.string.file_more_elements);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
        View v = convertView;
        
        if (v == null) {

            LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.filerow, null);
            
            v.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					File f = (File)v.getTag();
					if (((FileExplorerActivity)FileArrayAdapter.this.getContext()).setDirectory(f)) return;
					if (((FileExplorerActivity)FileArrayAdapter.this.getContext()).setPreviewFile(f)) return;
				}
			});
            
            v.setOnTouchListener(new OnTouchListener() {
				public boolean onTouch(View v, MotionEvent event) {
					current = (File)v.getTag();
					return false;
				}
			});
            
            v.setOnLongClickListener(new OnLongClickListener() {
				public boolean onLongClick(View v) {
					current = (File)v.getTag();
					return false;
				}
			});
            
            v.setOnCreateContextMenuListener(((FileExplorerActivity)FileArrayAdapter.this.getContext()));
            
        }
        
        EnhancedFile f = new EnhancedFile(getItem(position));
        
        if (f != null) {
        	
            FileItem fi = new FileItem(v, position);
            fi.configure(mod);
            fi.show(f);
        	v.setTag(f);
        	
        }
        
		return v;
		
	}

	public File getCurrent() {
		return current;
	}

	public void setMod(FileItemModeler modsEnum) {
		this.mod = modsEnum;
	}

}
