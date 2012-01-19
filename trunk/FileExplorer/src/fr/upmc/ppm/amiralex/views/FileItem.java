package fr.upmc.ppm.amiralex.views;

import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import fr.upmc.ppm.amiralex.R;
import fr.upmc.ppm.amiralex.tools.EnhancedFile;
import fr.upmc.ppm.amiralex.tools.Utils;

public class FileItem {
	
	public final View v;
	public final TextView label, labelMore, labelDate, labelRights;
	public final ImageView icon;
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
	
	public void configure(FileItemModeler mod) {

		int list = 2;
		switch (mod) {
		case GRID:
			icon.setMinimumWidth(150);
			v.setBackgroundColor(i % 2 == 0 ? Color.LTGRAY : Color.GRAY);
		case LIST_LIGHT: --list;
		case LIST: --list;
		case LIST_ADVANCED:
			if (mod != FileItemModeler.GRID) {
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
        long elements = f.getLength();
        
        label.setText(textLabel);
        labelDate.setText("(" + f.lastModified("d/MM/y", v.getContext().getResources().getString(R.string.unknown)) + ")");
        labelRights.setText(f.getRights());
        labelMore.setText(!f.isDirectory() 
        		? Utils.formatSize(elements, 0) 
        		: (elements == 0 
        			? FileArrayAdapter.emptyString 
        			: elements + " " + FileArrayAdapter.elementsString));
        icon.setImageResource(f.getImageRessource());
        icon.refreshDrawableState();
        
	}

}