package org.johnnei.javatorrent.internal.utils;

@FunctionalInterface
public interface CheckedRunnable<E extends Exception> {

	void run() throws E;

}
