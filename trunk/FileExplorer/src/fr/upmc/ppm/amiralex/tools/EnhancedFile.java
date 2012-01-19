package fr.upmc.ppm.amiralex.tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

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
			return "";
		String[] tmp = getName().split("\\.");
		return tmp.length == 1 ? "bin" : tmp[tmp.length - 1].toLowerCase();
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
	
	public String getRights() {
		return (isDirectory() ? "d" : "")
				+ (canRead() ? "r" : "-")
				+ (canWrite() ? "w" : "-")
				+ (canExecute() ? "x" : "-");
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
		return deleteCascade(this);
	}
	
	public static boolean deleteCascade(File f) {
		
		return walkFiles(new FileWalker<Boolean>() {
			public Boolean walk(File f, Boolean result) {
				if (!f.delete()) result = false;
				return result;
			}
		}, f, true);
		
		/* petite fonction récursive pour la frime
		if (f == null) return false;
		if (!f.isDirectory()) return f.delete();
		for (File files : f.listFiles())
			deleteCascade(files);
		return f.delete();*/
		
	}
	
	public static int countFiles(File f) {
		
		return walkFiles(new FileWalker<Integer>() {
			public Integer walk(File f, Integer result) {
				return result+1;
			}
		}, f, 0);
		
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

	public List<File> listAllFiles() {
		return listAllFiles(new ArrayList<File>(), this);
	}
	
	public List<File> listAllFiles(ArrayList<File> files, File f) {
		if (f == null) return files;
		for (File fs : f.listFiles())
			listAllFiles(files, fs);
		files.add(f);
		return files;
	}
	
	public static <Result> Result walkFiles(FileWalker<Result> fw, File f, Result r) {
		if (f == null) return null;
		for (File fs : f.listFiles())
			walkFiles(fw, fs, r);
		return fw.walk(f, r);
	}
	
	public static interface FileWalker<Result> {
		public Result walk(File f, Result r);
	}

}
