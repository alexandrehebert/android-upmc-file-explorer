package fr.upmc.ppm.amiralex.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

import fr.upmc.ppm.amiralex.tools.FileTypeResolver.FileType;

/**
 * 
 * @author alexandre hebert
 *
 */
public class EnhancedFile extends File {

	private static final long serialVersionUID = 5826959705298825689L;
	private static final FileTypeResolver resolver = new FileTypeResolver();

	public EnhancedFile(String path) {
		super(path);
	}

	public EnhancedFile(File path) {
		super(path.getAbsolutePath());
	}

	public String getExt() {
		if (isDirectory())
			return "dir";
		String[] tmp = getName().split("\\.");
		return tmp.length == 1 ? "bin" : tmp[tmp.length - 1];
	}

	public FileType getType() {
		return resolver.searchType(this);
	}

	public long getLength() {
		return (!isDirectory() ? length()
				: (listFiles() != null ? listFiles().length : 0));
	}

	public int getImageRessource() {
		return getType().getImageRessource();
	}
	
	public String getRealAbsolutePath() {
		if (isDirectory()) return getAbsolutePath();
		return getAbsolutePath().substring(0, getAbsolutePath().length() - getName().length());
	}
	
	private File generateUniquePath(File to) {
		
		int i = 0;
		String path = new EnhancedFile(to).getRealAbsolutePath();
		do 
			to = new File(path + "/" + (i++ == 0 ? "" : (i++) + "_") + getName());
		while (to.exists());
		return to;
		
	}
	
	public boolean deleteCascade() {
		return _delete_cascade(this);
	}
	
	public boolean _delete_cascade(File f) {
		
		// petite fonction récursive pour la frime
		if (!f.isDirectory()) return f.delete();
		for (File files : f.listFiles())
			return _delete_cascade(files);
		return f.delete();
		
	}

	public boolean copyTo(File to) {

		FileChannel in = null; // canal d'entrée
		FileChannel out = null; // canal de sortie
		
		to = generateUniquePath(to);

		try {

			in = new FileInputStream(this).getChannel();
			out = new FileOutputStream(to).getChannel();

			in.transferTo(0, in.size(), out);

			if (in != null)
				in.close();
			if (out != null)
				out.close();

			return true;

		} catch (Exception e) {
			return false;
		}

		/*
		 * try {
		 * 
		 * int i = 2; File tmp = to; while (tmp.exists()) tmp = new
		 * File(to.getAbsolutePath() + "/" + (i++) + "_" + getName()); to = tmp;
		 * 
		 * BufferedReader br = new BufferedReader(new FileReader(this));
		 * BufferedWriter bw = new BufferedWriter(new FileWriter(to)); String
		 * line = null; while ((line = br.readLine()) != null) { bw.write(line);
		 * }
		 * 
		 * return true;
		 * 
		 * } catch (Exception e) { return false; }
		 */
	}

	public boolean moveTo(File to) {

		to = generateUniquePath(to);
		
		return renameTo(to);
		
	}

}
