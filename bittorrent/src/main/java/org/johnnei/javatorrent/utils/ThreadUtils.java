package org.johnnei.javatorrent.utils;

public class ThreadUtils {

	/**
	 *
	 * @param ms
	 * @Deprecated The interrupted exception should be honoured instead of ignored.
	 */
	@Deprecated
	public static void sleep(int ms) {
		if (ms <= 0) {
			return;
		}
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

}
