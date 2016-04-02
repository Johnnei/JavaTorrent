package org.johnnei.javatorrent.torrent.download.tracker;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerFactory;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.tracker.IPeerConnector;
import org.johnnei.javatorrent.internal.tracker.TrackerManager;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;

@RunWith(EasyMockRunner.class)
public class TrackerManagerTest extends EasyMockSupport {

	private TrackerManager cut;

	@Mock
	private IPeerConnector peerConnectorMock;

	@Mock
	private TrackerFactory trackerFactoryMock;

	@Before
	public void setUp() {
		cut = new TrackerManager(peerConnectorMock, trackerFactoryMock);
	}

	@Test
	public void testAnnounce() {
		Torrent torrentMock = createMock(Torrent.class);

		List<ITracker> trackers = Arrays.asList(createMock(ITracker.class), createMock(ITracker.class));
		trackers.forEach(trackerMock -> {
			trackerMock.announce(same(torrentMock));
			expectLastCall();
		});

		expect(trackerFactoryMock.getTrackersHavingTorrent(same(torrentMock))).andReturn(trackers);
		replayAll();

		cut.announce(torrentMock);

		verifyAll();
	}

	@Test
	public void testGetConnectingCountFor() {
		Torrent torrentMock = createMock(Torrent.class);
		final int connectingCount = 5;

		expect(peerConnectorMock.getConnectingCountFor(same(torrentMock))).andReturn(connectingCount);
		replayAll();

		int connecting = cut.getConnectingCountFor(torrentMock);

		Assert.assertEquals("Incorrect connecting amount", connectingCount, connecting);
	}

	@Test
	public void testAddTorrent() {
		Torrent torrent = createMock(Torrent.class);
		ITracker trackerMock = createMock(ITracker.class);
		final String trackerUrl = "udp://localhost:80";
		final String missingTrackerUrl = "udp://localhost:8080";

		expect(trackerFactoryMock.getTrackerFor(eq(trackerUrl))).andReturn(Optional.of(trackerMock));
		expect(trackerFactoryMock.getTrackerFor(eq(missingTrackerUrl))).andReturn(Optional.empty());
		trackerMock.addTorrent(same(torrent));

		replayAll();
		cut.addTorrent(torrent, missingTrackerUrl);
		cut.addTorrent(torrent, trackerUrl);

		verifyAll();
	}

}
