package fr.upmc.ppm.amiralex.views;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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

	private String emptyString, elementsString;
	private File current;
	private TypeMod mod;

	public FileArrayAdapter(Context context, int textViewResourceId,
			File[] objects, TypeMod mod) {
		super(context, textViewResourceId, objects);
		emptyString = context.getResources()
				.getString(R.string.file_more_empty);
		elementsString = context.getResources().getString(
				R.string.file_more_elements);
		this.mod = mod;
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

	public void setMod(TypeMod modsEnum) {
		this.mod = modsEnum;
	}

	public class FileItem {
		
		View v;
		TextView label, labelMore, labelDate, labelRights;
		ImageView icon;
		int i;

		public FileItem(View v, int i) {
			this.v = v;
			this.i = i;
			
			label = (TextView) v.findViewById(R.id.file);
			labelMore = (TextView) v.findViewById(R.id.fileMore);
			labelDate = (TextView) v.findViewById(R.id.fileDate);
			labelRights = (TextView) v.findViewById(R.id.fileRights);
			icon = (ImageView) v.findViewById(R.id.icon);
		}
		
		public void configure(TypeMod mod) {

			int list = 2;
			switch (mod) {
			case GRID:
				icon.setMinimumWidth(150);
				v.setBackgroundColor(i % 2 == 0 ? Color.LTGRAY : Color.GRAY);
			case LIST_LIGHT: --list;
			case LIST: --list;
			case LIST_ADVANCED:
				if (mod != TypeMod.GRID) {
					icon.setMinimumWidth(30);
					v.setBackgroundColor(Color.TRANSPARENT);
				}
				labelMore.setVisibility(list > 0 ? View.VISIBLE : View.GONE);
				labelDate.setVisibility(list > 1 ? View.VISIBLE : View.GONE);
				labelRights.setVisibility(list > 1 ? View.VISIBLE : View.GONE);
				break;
			}
			
		}
		
		public void show(EnhancedFile f) {

            String textLabel = f.getName();
            SimpleDateFormat sdf = new SimpleDateFormat("(d/MM/y)");
            long elements = new EnhancedFile(f).getLength();
            
            label.setText(textLabel);
            labelDate.setText(sdf.format(new Date(f.lastModified())));
            labelRights.setText(f.getRights());
            labelMore.setText(!f.isDirectory() ? Utils.formatSize(elements, 0) : (elements == 0 ? emptyString : elements + " " + elementsString));
            icon.setImageResource(f.getImageRessource());
            icon.refreshDrawableState();
            
		}

	}

}
