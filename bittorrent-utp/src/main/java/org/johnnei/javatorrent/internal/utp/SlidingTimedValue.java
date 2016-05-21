package org.johnnei.javatorrent.internal.utp;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Created by johnn on 21/05/2016.
 */
public class SlidingTimedValue<T> {

	private Clock clock = Clock.systemDefaultZone();

	private TreeMap<T, Instant> values;

	public SlidingTimedValue() {
		values = new TreeMap<>();
	}

	public void addValue(T value) {
		values.put(value, clock.instant());
	}

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
