package org.johnnei.javatorrent.utils;

@FunctionalInterface
public interface CheckedRunnable<E extends Exception> {

	void run() throws E;

}
