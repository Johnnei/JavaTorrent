package org.johnnei.javatorrent.internal.tracker.http;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.ExecutorServiceMock;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.tracker.IPeerConnector;

import static org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent.EVENT_NONE;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link HttpTracker}
 */
public class HttpTrackerTest {

	@Test
	public void testScrape() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);

		HttpTracker cut = new HttpTracker.Builder()
				.setTorrentClient(torrentClientMock)
				.setUrl("http://tracker.localhost:8989/announce")
				.build();

		assertThrows(UnsupportedOperationException.class, cut::scrape);
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

	@Test
	public void testAddHasTorrent() {
		Torrent torrentOne = DummyEntity.createUniqueTorrent();
		Torrent torrentTwo = DummyEntity.createUniqueTorrent(torrentOne);

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		HttpTracker cut = new HttpTracker.Builder()
				.setTorrentClient(torrentClientMock)
				.setUrl("http://tracker.localhost:8989/announce")
				.build();

		assertFalse(cut.hasTorrent(torrentOne), "Torrent one should not be available yet.");
		assertFalse(cut.hasTorrent(torrentTwo), "Torrent two should not be available yet.");
		cut.addTorrent(torrentOne);
		assertTrue(cut.hasTorrent(torrentOne), "Torrent one should be available.");
		assertPresent("Torrent one info should have been created.", cut.getInfo(torrentOne));
		assertFalse(cut.hasTorrent(torrentTwo), "Torrent two should not be available yet.");
		cut.addTorrent(torrentTwo);
		assertTrue(cut.hasTorrent(torrentOne), "Torrent one should be available.");
		assertPresent("Torrent two info should have been created.", cut.getInfo(torrentTwo));
		assertTrue(cut.hasTorrent(torrentTwo), "Torrent two should be available.");
	}

	@Test
	public void testAddTorrentDuplicate() {
		Torrent torrent = DummyEntity.createUniqueTorrent();
		TorrentClient torrentClientMock = mock(TorrentClient.class);

		HttpTracker cut = new HttpTracker.Builder()
				.setTorrentClient(torrentClientMock)
				.setUrl("http://tracker.localhost:8989/announce")
				.build();

		cut.addTorrent(torrent);
		assertTrue(cut.hasTorrent(torrent), "Torrent should not be available yet.");
		assertPresent("Torrent one info should have been created.", cut.getInfo(torrent));

		TorrentInfo info = cut.getInfo(torrent).get();
		info.setEvent(EVENT_NONE);

		cut.addTorrent(torrent);

		info = cut.getInfo(torrent).get();
		assertEquals(EVENT_NONE, info.getEvent(), "Torrent state should not have been reset.");
	}

	@Test
	public void testConnectPeer() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		IPeerConnector peerConnectorMock = mock(IPeerConnector.class);

		when(torrentClientMock.getPeerConnector()).thenReturn(peerConnectorMock);

		PeerConnectInfo peerConnectInfo = mock(PeerConnectInfo.class);

		HttpTracker cut = new HttpTracker.Builder()
				.setTorrentClient(torrentClientMock)
				.setUrl("http://tracker.localhost:8989/announce")
				.build();

		cut.connectPeer(peerConnectInfo);

		verify(peerConnectorMock).enqueuePeer(same(peerConnectInfo));
	}

	@Test
	public void testAnnounceEndpointNotAvailable() throws IOException {
		TorrentClient torrentClientMock = mock(TorrentClient.class);

		when(torrentClientMock.getPeerId()).thenReturn(DummyEntity.createPeerId());
		when(torrentClientMock.getExecutorService()).thenReturn(new ExecutorServiceMock());

		Torrent torrent = DummyEntity.createUniqueTorrent();

		HttpTracker cut = new HttpTracker.Builder()
				.setTorrentClient(torrentClientMock)
				.setUrl(String.format("http://tracker.localhost:%d/announce", DummyEntity.findAvailableTcpPort()))
				.build();

		cut.addTorrent(torrent);
		cut.announce(torrent);

		assertEquals("Announce failed", cut.getStatus(), "Incorrect tracker status after announce error");
	}
}
