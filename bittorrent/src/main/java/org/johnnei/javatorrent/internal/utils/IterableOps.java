package org.johnnei.javatorrent.internal.utils;

import java.util.function.BiFunction;

public class IterableOps {

	/**
	 * Sequential reducing operation which allows for type transformation and empty collections.
	 * @param collection The collection to fold
	 * @param identity The identity value of <code>foldFn</code> operation (ie. has no impact on the result)
	 * @param foldFn The operating that takes the accumulated result and a new item to create a new accumulated state.
	 * @param <T> The type of elements in the collection to fold
	 * @param <R> The result type of the <code>foldFn</code> operation
	 * @return All elements of <code>collection</code> folded by <code>foldFn</code>
	 */
	public static <T, R> R foldLeft(Iterable<T> collection, R identity, BiFunction<R, T, R> foldFn) {
		R accumulated = identity;

		for (T item : collection) {
			accumulated = foldFn.apply(accumulated, item);
		}

		return accumulated;
	}

}
