package org.johnnei.javatorrent.bittorrent.tracker;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.utils.CheckedBiFunction;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TrackerFactory}
 */
public class TrackerFactoryTest {

	@Test
	public void testBuildFailureOnNoProtocols() {
		Exception e = assertThrows(IllegalStateException.class, () -> new TrackerFactory.Builder().build());
		assertThat(e.getMessage(), containsString("At least one tracker protocol"));
	}

	@Test
	public void testBuildFailureOnNoTorrentClient() {
		Exception e = assertThrows(IllegalStateException.class, () -> new TrackerFactory.Builder()
					.registerProtocol("udp", (url, client) -> mock(ITracker.class))
					.build());
		assertThat(e.getMessage(), containsString("Torrent client"));
	}

	@Test
	public void testBuildOverrideProtocol() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		ITracker trackerMock = mock(ITracker.class);
		ITracker trackerMockTwo = mock(ITracker.class);

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.registerProtocol("udp", (url, client) -> trackerMockTwo)
				.build();

		Optional<ITracker> result = cut.getTrackerFor("udp://localhost:80");

		assertTrue(result.isPresent(), "Tracker should have been present");
		assertEquals(trackerMockTwo, result.get(), "Incorrect tracker has been returned");
	}

	@Test
	public void testToString() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		final ITracker trackerMock = mock(ITracker.class);

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.build();

		String result = cut.toString();

		assertTrue(result.startsWith("TrackerFactory["));
	}

	@Test
	public void testGetTrackersHavingTorrent() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		ITracker trackerMock = mock(ITracker.class);
		Torrent torrent = DummyEntity.createUniqueTorrent();

		when(trackerMock.hasTorrent(eq(torrent))).thenReturn(false).thenReturn(true);

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.build();

		Optional<ITracker> tracker = cut.getTrackerFor("udp://localhost:80");
		assertTrue(tracker.isPresent(), "Tracker should have been added");

		assertEquals(0, cut.getTrackersHavingTorrent(torrent).size(), "No tracker should have the given torrent");
		assertEquals(1, cut.getTrackersHavingTorrent(torrent).size(), "Tracker should have the given torrent");
	}

	@Test
	public void testGetTrackerFor() {
		ITracker tracker = mock(ITracker.class);

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(mock(TorrentClient.class))
				.registerProtocol("udp", (url, client) -> tracker)
				.build();

		Optional<ITracker> result = cut.getTrackerFor("udp://localhost:80");

		assertTrue(result.isPresent(), "Tracker should have been present");
		assertEquals(tracker, result.get(), "Incorrect tracker has been returned");
	}

	@Test
	public void testGetTrackerForException() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> { throw new TrackerException("Test"); })
				.build();

		Optional<ITracker> result = cut.getTrackerFor("udp://localhost:80");

		assertFalse(result.isPresent(), "Tracker should have been present");
	}

	@Test
	public void testGetTrackerForCaching() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		final ITracker trackerMock = mock(ITracker.class);

		final String trackerUrl = "udp://localhost:80";

		CheckedBiFunction<String, TorrentClient, ITracker, TrackerException> supplierMock = mock(CheckedBiFunction.class);
		when(supplierMock.apply(eq(trackerUrl), same(torrentClientMock))).thenReturn(trackerMock);

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", supplierMock)
				.build();

		Optional<ITracker> result = cut.getTrackerFor(trackerUrl);
		Optional<ITracker> resultTwo = cut.getTrackerFor(trackerUrl);

		assertTrue(result.isPresent(), "Tracker should have been present");
		assertTrue(resultTwo.isPresent(), "Second tracker should have been present");
		assertEquals(trackerMock, result.get(), "Incorrect tracker has been returned");
		assertTrue(resultTwo.get() == result.get(), "Trackers should have been the same instance");
	}

	@Test
	public void testGetTrackerForUnparsableUrl() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		ITracker trackerMock = mock(ITracker.class);

		TrackerFactory cut = new TrackerFactory.Builder()
			.setTorrentClient(torrentClientMock)
			.registerProtocol("udp", (url, client) -> trackerMock)
			.build();

		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.getTrackerFor("not_a_valid_url"));
		assertThat(e.getMessage(), containsString("protocol definition"));
	}

	@Test
	public void testGetTrackerForUnsupportedProtocol() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		ITracker trackerMock = mock(ITracker.class);

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.build();

		assertThat(cut.getTrackerFor("http://example.com").isPresent(), is(false));
	}

}
