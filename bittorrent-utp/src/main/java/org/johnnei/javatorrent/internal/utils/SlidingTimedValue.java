package org.johnnei.javatorrent.internal.utils;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A class which tracks the minimum value out of a set of values within a given time window. The data is ordered by using a {@link TreeMap}.
 *
 * @param <T> The type of data to store.
 */
public class SlidingTimedValue<T> {

	private Clock clock = Clock.systemDefaultZone();

	private TreeMap<T, Instant> values;

	/**
	 * Creates a new instance to track the minimum value.
	 */
	public SlidingTimedValue() {
		values = new TreeMap<>();
	}

	/**
	 * Registers a value to be considered the minimum. The item will expire in two minutes from being called.
	 * @param value The value to add.
	 */
	public void addValue(T value) {
		values.put(value, clock.instant());
	}

	/**
	 * Finds the minimum value of the set of values.
	 * @return The minimum value.
	 */
	public T getMinimum() {
		// Remove all values which are no longer recent
		Collection<Map.Entry<T, Instant>> entriesToRemove = new ArrayList<>();
		final Instant limit = clock.instant().minus(2, ChronoUnit.MINUTES);
		entriesToRemove.addAll(values.entrySet().stream().filter(entry -> entry.getValue().isBefore(limit)).collect(Collectors.toList()));
		entriesToRemove.stream().map(Map.Entry::getKey).forEach(values::remove);

		// Get the minimum value key.
		return values.firstKey();
	}

}
