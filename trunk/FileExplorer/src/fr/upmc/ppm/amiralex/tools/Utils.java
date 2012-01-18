package fr.upmc.ppm.amiralex.tools;

import java.text.NumberFormat;

/**
 * 
 * @author alexandre hebert
 *
 */
public class Utils {
	public static String formatSize(long longSize, int decimalPos) {
		NumberFormat fmt = NumberFormat.getNumberInstance();
		if (decimalPos >= 0) {
			fmt.setMaximumFractionDigits(decimalPos);
		}
		final double size = longSize;
		double val = size / (1024 * 1024);
		if (val > 1) {
			return fmt.format(val).concat(" MB");
		}
		val = size / 1024;
		if (val > 10) {
			return fmt.format(val).concat(" KB");
		}
		return fmt.format(val).concat(" bytes");
	}
}