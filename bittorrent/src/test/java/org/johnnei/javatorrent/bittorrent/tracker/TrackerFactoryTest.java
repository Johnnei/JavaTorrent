package org.johnnei.javatorrent.bittorrent.tracker;

import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.utils.CheckedBiFunction;

import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link TrackerFactory}
 */
public class TrackerFactoryTest extends EasyMockSupport {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testBuildFailureOnNoProtocols() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("At least one tracker protocol");

		new TrackerFactory.Builder().build();
	}

	@Test
	public void testBuildFailureOnNoTorrentClient() {
		thrown.expect(IllegalStateException.class);
		thrown.expectMessage("Torrent client");

		final ITracker trackerMock = createMock(ITracker.class);

		replayAll();

		try {
			new TrackerFactory.Builder()
					.registerProtocol("udp", (url, client) -> trackerMock)
					.build();
		} finally {
			verifyAll();
		}
	}

	@Test
	public void testBuildOverrideProtocol() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		final ITracker trackerMock = createMock(ITracker.class);
		final ITracker trackerMockTwo = createMock(ITracker.class);

		replayAll();

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.registerProtocol("udp", (url, client) -> trackerMockTwo)
				.build();

		Optional<ITracker> result = cut.getTrackerFor("udp://localhost:80");

		verifyAll();

		assertTrue("Tracker should have been present", result.isPresent());
		assertEquals("Incorrect tracker has been returned", trackerMockTwo, result.get());
	}

	@Test
	public void testToString() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		final ITracker trackerMock = createMock(ITracker.class);

		replayAll();

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.build();

		String result = cut.toString();

		verifyAll();

		assertTrue(result.startsWith("TrackerFactory["));
	}

	@Test
	public void testGetTrackersHavingTorrent() throws Exception {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		final ITracker trackerMock = createMock(ITracker.class);
		Torrent torrent = DummyEntity.createUniqueTorrent();

		expect(trackerMock.hasTorrent(eq(torrent))).andReturn(false);
		expect(trackerMock.hasTorrent(eq(torrent))).andReturn(true);

		replayAll();

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.build();

		Optional<ITracker> tracker = cut.getTrackerFor("udp://localhost:80");
		assertTrue("Tracker should have been added", tracker.isPresent());

		assertEquals("No tracker should have the given torrent", 0, cut.getTrackersHavingTorrent(torrent).size());
		assertEquals("No tracker should have the given torrent", 1, cut.getTrackersHavingTorrent(torrent).size());

		verifyAll();
	}

	@Test
	public void testGetTrackerFor() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		final ITracker trackerMock = createMock(ITracker.class);

		replayAll();

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.build();

		Optional<ITracker> result = cut.getTrackerFor("udp://localhost:80");

		verifyAll();

		assertTrue("Tracker should have been present", result.isPresent());
		assertEquals("Incorrect tracker has been returned", trackerMock, result.get());
	}

	@Test
	public void testGetTrackerForException() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);

		replayAll();

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> { throw new TrackerException("Test"); })
				.build();

		Optional<ITracker> result = cut.getTrackerFor("udp://localhost:80");

		verifyAll();

		assertFalse("Tracker should have been present", result.isPresent());
	}

	@Test
	public void testGetTrackerForCaching() throws Exception {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		final ITracker trackerMock = createMock(ITracker.class);

		final String trackerUrl = "udp://localhost:80";

		CheckedBiFunction<String, TorrentClient, ITracker, TrackerException> supplierMock = createMock(CheckedBiFunction.class);
		expect(supplierMock.apply(eq(trackerUrl), same(torrentClientMock))).andReturn(trackerMock);

		replayAll();

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", supplierMock)
				.build();

		Optional<ITracker> result = cut.getTrackerFor(trackerUrl);
		Optional<ITracker> resultTwo = cut.getTrackerFor(trackerUrl);

		verifyAll();

		assertTrue("Tracker should have been present", result.isPresent());
		assertTrue("Second tracker should have been present", resultTwo.isPresent());
		assertEquals("Incorrect tracker has been returned", trackerMock, result.get());
		assertTrue("Trackers should have been the same instance", resultTwo.get() == result.get());
	}

	@Test
	public void testGetTrackerForInvalidUrl() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("protocol definition");

		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		final ITracker trackerMock = createMock(ITracker.class);

		replayAll();

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.build();

		cut.getTrackerFor("not_a_valid_url");

		verifyAll();
	}

	@Test
	public void testGetTrackerForUnknownProtocol() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Unsupported protocol");

		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		final ITracker trackerMock = createMock(ITracker.class);

		replayAll();

		TrackerFactory cut = new TrackerFactory.Builder()
				.setTorrentClient(torrentClientMock)
				.registerProtocol("udp", (url, client) -> trackerMock)
				.build();

		cut.getTrackerFor("http://example.com");

		verifyAll();
	}
}