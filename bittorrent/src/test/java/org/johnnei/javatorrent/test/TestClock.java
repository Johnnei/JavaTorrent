package org.johnnei.javatorrent.test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A clock implementation which returns its values based on the supplied clock.
 * The supplied clock maybe be changed.
 *
 * @implNote
 * This should only be used in a testing environment.
 * The changing underlying clock breaks the {@link Clock} contract.
 *
 */
public class TestClock extends Clock {

	private static final Logger LOGGER = LoggerFactory.getLogger(TestClock.class);

	private Clock actualClock;

	public TestClock(Clock actualClock) {
		setClock(actualClock);
	}

	@Override
	public ZoneId getZone() {
		return actualClock.getZone();
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return actualClock.withZone(zone);
	}

	@Override
	public Instant instant() {
		return actualClock.instant();
	}

	public void setClock(Clock actualClock) {
		this.actualClock = Objects.requireNonNull(actualClock, "Clock cannot be null.");
		LOGGER.debug("Time is now: {}", actualClock.instant());
	}

}
