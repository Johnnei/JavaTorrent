package org.johnnei.javatorrent.internal.utils;

/**
 * Created by johnn on 21/05/2016.
 */
public class MathUtils {

	public static double clamp(double min, double max, double value) {
		return Math.min(Math.max(min, value), max);
	}
}
