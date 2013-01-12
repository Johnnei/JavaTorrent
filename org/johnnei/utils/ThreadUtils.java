package org.johnnei.utils;

public class ThreadUtils {

	public static void sleep(int ms) {
		if (ms <= 0)
			return;
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

}
