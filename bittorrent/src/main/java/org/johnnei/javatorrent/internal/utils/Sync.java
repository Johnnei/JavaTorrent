package org.johnnei.javatorrent.internal.utils;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * Utilities which make locking tasks cleaner.
 */
public class Sync {

	private Sync() {
		/* No utility instance for you */
	}

	/**
	 * Locks with the given lock and calls signalAll on the given condition.
	 * @param lock The lock to lock on
	 * @param condition The condition to signal
	 *
	 * @see Condition#signalAll()
	 */
	public static void signalAll(Lock lock, Condition condition) {
		lock.lock();
		try {
			condition.signalAll();
		} finally {
			lock.unlock();
		}
	}
}
