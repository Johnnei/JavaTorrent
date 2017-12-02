package org.johnnei.javatorrent.tracker;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests {@link UncappedDistributor}
 */
public class UncappedDistributorTest {
	@Test
	public void testHasReachedPeerLimit() throws Exception {
		assertFalse(new UncappedDistributor().hasReachedPeerLimit(null), "Limit should never be reached");
	}

}
