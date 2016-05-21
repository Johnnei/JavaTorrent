package org.johnnei.javatorrent.internal.utils;

/**
 * Created by johnn on 21/05/2016.
 */
public class MathUtils {

	public static float clamp(float min, float max, float value) {
		return Math.min(Math.max(min, value), max);
	}
}
