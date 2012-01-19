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

	/**
	 * Liste de correspondance entre le type mime et les extentions de fichiers
	 * Cela nous permet, pour une extention donnée, d'envoyer le bon intent sur le bus
	 */
	private List<FileType> exts = new ArrayList<FileType>();
	
	public FileTypeResolver() {
		exts.add(new FileType(R.string.type_image, "image/*", R.drawable.image, "jpg", "png", "gif"));
		exts.add(new FileType(R.string.type_audio, "audio/*", R.drawable.audio, "mp3", "aac", "ogg"));
		exts.add(new FileType(R.string.type_video, "video/*", R.drawable.video, "avi", "mov", "flv", "mp4"));
		exts.add(new FileType(R.string.type_internet, "text/html", R.drawable.internet, "htm", "html"));
		exts.add(new FileType(R.string.type_internet, null, R.drawable.internet, "xml"));
		exts.add(new FileType(R.string.type_text, "text/plain", R.drawable.text, "txt"));
	}
	
	/**
	 * On parcours la liste des FileType pour trouver celui
	 * qui correspond le mieux au fichier transmis
	 * 
	 * @param f
	 * @return
	 */
	public FileType searchType(File f) {
		if (!f.isDirectory()) {
			for (FileType ft : exts)
				if (ft.match(f.getAbsolutePath()))
					return ft;
			return new FileType(R.string.type_unknown, null, R.drawable.unknown);
		}
		return new FileType(R.string.type_folder, null, R.drawable.folder);
	}
	
	/**
	 * Classe de description d'un type mime particulier
	 */
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
		
		/**
		 * Retourne true si l'extention est gérées par ce type mime
		 * 
		 * @param fileName
		 * @return
		 */
		public boolean match(String fileName) {
			String[] tmpExt = fileName.split("\\.");
			String ext = tmpExt[tmpExt.length-1];
			return (exts.contains(ext.toLowerCase()));
		}
		
		/**
		 * Retourne la chaine de caractère correspondant à ce type mime
		 */
		public String getType(Context c) {
			return c.getResources().getString(type);
		}
		
		/**
		 * Indique si le type mime peut être ouvert par une application android
		 * si aucun type mime n'est transmis, dans ce cas on ouvre simplement la fénêtre de détails
		 * 
		 * @return
		 */
		public boolean isPreviewable() {
			// return exts.size() > 0;
			return mime != null;
		}
		
		/**
		 * Créé l'intent qui sera publié sur le bus
		 * pour lancer la preview du fichier
		 * 
		 * @param f
		 * @return
		 */
		public Intent toIntent(File f) {
			Intent intent = new Intent();
			intent.setAction(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(f), mime);
			return intent;
		}
		
		/**
		 * On ne s'en sert pas
		 * mais il parait que ça ouvre le menu de partage (sms, bluetooth, etc)
		 * 
		 * @param f
		 * @return
		 */
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
		
		/**
		 * Toutes les extentions rattachées à ce type mime auront le même icone
		 * @return
		 */
		public int getImageRessource() {
			return resource;
		}
		
	}

	
}
