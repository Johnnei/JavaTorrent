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

	public static void wait(Object waitObject) {
		synchronized (waitObject) {
			try {
				waitObject.wait();
			} catch (InterruptedException e) {
			}
		}
	}
	
	public static void notify(Object waitObject) {
		synchronized (waitObject) {
			waitObject.notify();
		}
	}
	
	public static void notifyAll(Object waitObject) {
		synchronized (waitObject) {
			waitObject.notifyAll();
		}
	}

}
