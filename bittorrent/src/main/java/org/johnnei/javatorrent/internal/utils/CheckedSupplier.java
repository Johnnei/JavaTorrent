package org.johnnei.javatorrent.internal.utils;

/**
 * A supplier which can throw a checked exception.
 */
@FunctionalInterface
public interface CheckedSupplier<T, E extends Exception> {

	T get() throws E;
}
