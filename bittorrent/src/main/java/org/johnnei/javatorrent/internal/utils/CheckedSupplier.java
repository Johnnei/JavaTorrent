package org.johnnei.javatorrent.internal.utils;

/**
 * Created by johnn on 09/03/2016.
 */
public interface CheckedSupplier<T, E extends Exception> {

	T get() throws E;
}
