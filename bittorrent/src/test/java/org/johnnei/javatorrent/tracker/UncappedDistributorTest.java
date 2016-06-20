package org.johnnei.javatorrent.tracker;

import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * Tests {@link UncappedDistributor}
 */
public class UncappedDistributorTest {
	@Test
	public void testHasReachedPeerLimit() throws Exception {
		assertFalse("Limit should never be reached", new UncappedDistributor().hasReachedPeerLimit(null));
	}

}