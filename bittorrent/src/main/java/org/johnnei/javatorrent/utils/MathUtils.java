package org.johnnei.javatorrent.utils;

public class MathUtils {

	private MathUtils() {
		/* No utility classes for you! */
	}

	public static int ceilDivision(int value, int divisor) {
		return (value + (divisor - 1)) / divisor;
	}

	public static long ceilDivision(long value, long divisor) {
		return (value + (divisor - 1)) / divisor;
	}

}
