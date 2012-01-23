package fr.upmc.ppm.amiralex.views;

import java.io.File;
import java.util.Comparator;

import fr.upmc.ppm.amiralex.tools.EnhancedFile;

public enum FileItemSorter implements Comparator<File> {
	
	// ne pas modifier l'ordre (donc les indices) !!.. très important, un peu codé à l'arrache mais bon...
	// les ids correspondent aux tuples du string-array du fichier de ressources
	BY_DATE, BY_EXT, BY_NAME, BY_TYPE, BY_DONT_CARE;

	public int compare(File f1, File f2) {
		switch (this) {
		case BY_DATE:
			return (int) (f1.lastModified() - f2.lastModified());
		case BY_EXT:
			return new EnhancedFile(f1).getExt()
					.compareTo(new EnhancedFile(f2).getExt());
		case BY_TYPE:
			if (f1.isDirectory() && !f2.isDirectory()) return -1;
			if (f2.isDirectory() && !f1.isDirectory()) return 1;
		case BY_NAME:
			return f1.getAbsolutePath().toLowerCase()
					.compareTo(f2.getAbsolutePath().toLowerCase());
		}
		return 0;
	}

}

/*
 * public class FileArrayComparators { public FileArrayComparators() {
 * comparators.put(0, BY_DATE); comparators.put(1, BY_EXT); comparators.put(2,
 * BY_NAME); } }
 */
