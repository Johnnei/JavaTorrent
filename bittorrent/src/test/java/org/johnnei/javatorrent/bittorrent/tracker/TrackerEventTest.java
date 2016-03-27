package org.johnnei.javatorrent.bittorrent.tracker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link TrackerEvent}
 */
public class TrackerEventTest {

	@Test
	public void testConnect() {
		assertEquals(0, TrackerEvent.EVENT_NONE.getId());
	}

	@Test
	public void testAnnounce() {
		assertEquals(1, TrackerEvent.EVENT_COMPLETED.getId());
	}

	@Test
	public void testScrape() {
		assertEquals(2, TrackerEvent.EVENT_STARTED.getId());
	}

	@Test
	public void testError() {
		assertEquals(3, TrackerEvent.EVENT_STOPPED.getId());
	}

}