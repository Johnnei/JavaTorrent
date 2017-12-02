package org.johnnei.javatorrent.torrent.download.tracker;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerFactory;
import org.johnnei.javatorrent.internal.tracker.TrackerManager;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.tracker.IPeerConnector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TrackerManagerTest {

	private TrackerManager cut;

	private IPeerConnector peerConnectorMock;

	private TrackerFactory trackerFactoryMock;

	@BeforeEach
	public void setUp() {
		peerConnectorMock = mock(IPeerConnector.class);
		trackerFactoryMock = mock(TrackerFactory.class);
		cut = new TrackerManager(peerConnectorMock, trackerFactoryMock);
	}

	@Test
	public void testAnnounce() {
		Torrent torrentMock = mock(Torrent.class);

		List<ITracker> trackers = Arrays.asList(mock(ITracker.class), mock(ITracker.class));

		when(trackerFactoryMock.getTrackersHavingTorrent(same(torrentMock))).thenReturn(trackers);

		cut.announce(torrentMock);

		trackers.forEach(trackerMock -> verify(trackerMock).announce(same(torrentMock)));
	}

	@Test
	public void testGetConnectingCountFor() {
		Torrent torrentMock = mock(Torrent.class);
		final int connectingCount = 5;

		when(peerConnectorMock.getConnectingCountFor(same(torrentMock))).thenReturn(connectingCount);

		int connecting = cut.getConnectingCountFor(torrentMock);

		assertEquals(connectingCount, connecting, "Incorrect connecting amount");
	}

	@Test
	public void testAddTorrent() {
		Torrent torrent = mock(Torrent.class);
		ITracker trackerMock = mock(ITracker.class);
		final String trackerUrl = "udp://localhost:80";
		final String missingTrackerUrl = "udp://localhost:8080";

		when(trackerFactoryMock.getTrackerFor(eq(trackerUrl))).thenReturn(Optional.of(trackerMock));
		when(trackerFactoryMock.getTrackerFor(eq(missingTrackerUrl))).thenReturn(Optional.empty());

		cut.addTorrent(torrent, missingTrackerUrl);
		cut.addTorrent(torrent, trackerUrl);

		verify(trackerMock).addTorrent(same(torrent));
	}

}
