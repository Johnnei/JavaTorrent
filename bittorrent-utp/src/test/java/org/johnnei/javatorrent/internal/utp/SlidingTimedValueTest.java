package org.johnnei.javatorrent.internal.utp;

import java.time.Clock;
import java.time.Duration;

import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link SlidingTimedValue}
 */
public class SlidingTimedValueTest {

	@Test
	public void testSingleItem() {
		SlidingTimedValue<Integer> cut = new SlidingTimedValue<>();
		cut.addValue(5);
		assertEquals((int) cut.getMinimum(), 5);
	}

	@Test
	public void testMultipleItems() {
		SlidingTimedValue<Integer> cut = new SlidingTimedValue<>();
		cut.addValue(5);
		cut.addValue(3);
		cut.addValue(6);
		cut.addValue(7);
		assertEquals((int) cut.getMinimum(), 3);
	}

	@Test
	public void testExpiredItem() {
		Clock clock = Clock.offset(Clock.systemDefaultZone(), Duration.ofMinutes(3));

		SlidingTimedValue<Integer> cut = new SlidingTimedValue<>();
		cut.addValue(5);

		Whitebox.setInternalState(cut, Clock.class, clock);

		cut.addValue(7);
		assertEquals((int) cut.getMinimum(), 7);
	}

}