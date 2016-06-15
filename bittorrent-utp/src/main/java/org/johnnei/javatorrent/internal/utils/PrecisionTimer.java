package org.johnnei.javatorrent.internal.utils;

/**
 * Created by johnn on 22/05/2016.
 */
public class PrecisionTimer {

	public int getCurrentMicros() {
		return (int) (System.nanoTime() / 1000L);
	}
}
