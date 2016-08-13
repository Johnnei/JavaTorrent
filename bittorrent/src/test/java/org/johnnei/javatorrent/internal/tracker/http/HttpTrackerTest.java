package org.johnnei.javatorrent.internal.tracker.http;

import org.johnnei.javatorrent.TorrentClient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link HttpTracker}
 */
public class HttpTrackerTest {

	@Test(expected = UnsupportedOperationException.class)
	public void testScrape() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);

		HttpTracker cut = new HttpTracker.Builder()
				.setTorrentClient(torrentClientMock)
				.setUrl("http://tracker.localhost:8989/announce")
				.build();

		cut.scrape();
	}

	@Test
	public void testGetName() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);

		HttpTracker cut = new HttpTracker.Builder()
				.setTorrentClient(torrentClientMock)
				.setUrl("http://tracker.localhost:8989/announce")
				.build();

		assertEquals("tracker.localhost", cut.getName());
	}
}
