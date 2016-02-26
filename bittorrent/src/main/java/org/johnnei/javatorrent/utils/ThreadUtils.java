package org.johnnei.javatorrent.utils;

import java.util.concurrent.locks.Condition;

public class ThreadUtils {

	public static void sleep(int ms) {
		if (ms <= 0) {
			return;
		}
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * @param waitObject
	 *
	 * @deprecated Replaced by {@link Condition#await()}
	 */
	@Deprecated
	public static void wait(Object waitObject) {
		synchronized (waitObject) {
			try {
				waitObject.wait();
			} catch (InterruptedException e) {
			}
		}
	}

	/**
	 * @param waitObject
	 *
	 * @deprecated Replaced by {@link Condition#signal()()}
	 */
	@Deprecated
	public static void notify(Object waitObject) {
		synchronized (waitObject) {
			waitObject.notify();
		}
	}

}
