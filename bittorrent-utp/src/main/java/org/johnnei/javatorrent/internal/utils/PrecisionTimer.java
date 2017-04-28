package org.johnnei.javatorrent.internal.utils;

/**
 * Class which provides high accuracy timestamps. The timestamps do not origin from a set point and should only be used for comparison.
 */
public class PrecisionTimer {

	/**
	 * @return The current time with microsecond accuracy.
	 */
	public int getCurrentMicros() {
		return (int) (System.nanoTime() / 1000L);
	}
}
