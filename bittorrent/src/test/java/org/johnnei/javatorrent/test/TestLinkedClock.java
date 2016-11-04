package org.johnnei.javatorrent.test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A clock which returns the clock instances in a given order.
 */
public class TestLinkedClock extends Clock {

	private Logger LOGGER = LoggerFactory.getLogger(TestLinkedClock.class);

	private Queue<Clock> clocks;

	private Clock clock;

	public TestLinkedClock(Clock... clocks) {
		this(new LinkedList<>(Arrays.asList(clocks)));
	}

	public TestLinkedClock(Queue<Clock> clocks) {
		this.clocks = clocks;
		this.clock = clocks.poll();
		LOGGER.debug("Time is now: {}", clock.instant());
	}

	@Override
	public ZoneId getZone() {
		return clock.getZone();
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return clock.withZone(zone);
	}

	@Override
	public Instant instant() {
		Instant instant = clock.instant();
		Clock newClock = clocks.poll();
		if (newClock != null) {
			clock = newClock;
			LOGGER.debug("Time is now: {}", clock.instant());
		}
		return instant;
	}
}
