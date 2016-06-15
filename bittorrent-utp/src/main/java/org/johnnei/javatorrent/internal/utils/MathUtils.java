package org.johnnei.javatorrent.internal.utils;

/**
 * Some helper methods for math operations.
 */
public class MathUtils {

	private MathUtils() {
		/* No utility class instances for you! */
	}

	/**
	 * Constrains the value between the given borders (inclusive).
	 * @param min The minimum value.
	 * @param max The maximum value.
	 * @param value The value to constrain within <code>min</code> and <code>max</code>
	 * @return The <code>value</code> if within <code>min</code> and <code>max</code>, otherwise one of <code>min</code> and <code>max</code>.
	 */
	public static double clamp(double min, double max, double value) {
		return Math.min(Math.max(min, value), max);
	}
}
