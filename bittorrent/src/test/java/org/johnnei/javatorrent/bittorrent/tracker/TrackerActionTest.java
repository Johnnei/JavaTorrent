package org.johnnei.javatorrent.bittorrent.tracker;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests {@link TrackerAction}
 */
public class TrackerActionTest {

	@Test
	public void testConnect() {
		assertEquals(TrackerAction.CONNECT, TrackerAction.of(0));
	}

	@Test
	public void testAnnounce() {
		assertEquals(TrackerAction.ANNOUNCE, TrackerAction.of(1));
	}

	@Test
	public void testScrape() {
		assertEquals(TrackerAction.SCRAPE, TrackerAction.of(2));
	}

	@Test
	public void testError() {
		assertEquals(TrackerAction.ERROR, TrackerAction.of(3));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testIncorrectId() {
		TrackerAction.of(4);
	}
}