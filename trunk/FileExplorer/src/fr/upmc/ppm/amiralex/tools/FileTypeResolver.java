package fr.upmc.ppm.amiralex.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import fr.upmc.ppm.amiralex.R;

/**
 * 
 * @author alexandre hebert
 *
 */
public class FileTypeResolver {

	private List<FileType> exts = new ArrayList<FileType>();
	
	public FileTypeResolver() {
		exts.add(new FileType(R.string.type_image, "image/*", R.drawable.image, "jpg", "png", "gif"));
		exts.add(new FileType(R.string.type_audio, "audio/*", R.drawable.audio, "mp3", "aac", "ogg"));
		exts.add(new FileType(R.string.type_video, "video/*", R.drawable.video, "avi", "mov", "flv", "mp4"));
		exts.add(new FileType(R.string.type_internet, "text/html", R.drawable.internet, "htm", "html"));
		exts.add(new FileType(R.string.type_internet, null, R.drawable.internet, "xml"));
		exts.add(new FileType(R.string.type_text, "text/plain", R.drawable.text, "txt"));
	}
	
	public FileType searchType(File f) {
		if (!f.isDirectory()) {
			for (FileType ft : exts)
				if (ft.match(f.getAbsolutePath()))
					return ft;
			return new FileType(R.string.type_unknown, null, R.drawable.unknown);
		}
		return new FileType(R.string.type_folder, null, R.drawable.folder);
	}
	
	public static class FileType {
		
		private int type;
		private List<String> exts;
		private int resource;
		private String mime;
		
		public FileType(int type, String mime, int resource, String... exts) {
			this.type = type;
			this.exts = new ArrayList<String>(Arrays.asList(exts));
			this.resource = resource;
			this.mime = mime;
		}
		
		public boolean match(String fileName) {
			String[] tmpExt = fileName.split("\\.");
			String ext = tmpExt[tmpExt.length-1];
			return (exts.contains(ext.toLowerCase()));
		}
		
		public String getType(Context c) {
			return c.getResources().getString(type);
		}
		
		public boolean isPreviewable() {
			// return exts.size() > 0;
			return mime != null;
		}
		
		public Intent toIntent(File f) {
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(f), mime);
			return intent;
		}
		
		public Intent toShareIntent(File f) {
		    Intent share = new Intent(Intent.ACTION_SEND);
		    share.setType(mime);
		    share.putExtra(Intent.EXTRA_STREAM,
		    Uri.parse("file://" + f.getAbsolutePath()));
		    return Intent.createChooser(share, "share");
		}
		
		public int getTypeRessource() {
			return type;
		}
		
		public int getImageRessource() {
			return resource;
		}
		
	}

	
}
