package org.johnnei.javatorrent.tracker;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link EqualDistributor}
 */
public class EqualDistributorTest {

	@Test
	public void testHasReachedPeerLimitTooManyTorrents() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);

		when(torrentClientMock.getTorrentCount()).thenReturn(6);

		EqualDistributor cut = new EqualDistributor(torrentClientMock, 5);
		assertFalse(cut.hasReachedPeerLimit(torrentMock), "Limit should not have been reached yet (0 < 1)");
	}

	@Test
	public void testHasReachedPeerLimitTooManyTorrentsOverLimit() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		List<Peer> collectionMock = mock(List.class);

		when(torrentClientMock.getTorrentCount()).thenReturn(6);
		when(torrentMock.getPeers()).thenReturn(collectionMock);
		when(collectionMock.size()).thenReturn(1);

		EqualDistributor cut = new EqualDistributor(torrentClientMock, 5);
		assertTrue(cut.hasReachedPeerLimit(torrentMock), "Limit should have been reached (1 >= 1)");
	}

	@Test
	public void testHasReachedPeerLimit() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);

		when(torrentClientMock.getTorrentCount()).thenReturn(1);

		EqualDistributor cut = new EqualDistributor(torrentClientMock, 5);
		assertFalse(cut.hasReachedPeerLimit(torrentMock), "Limit should not have been reached yet (1 < 5)");
	}

	@Test
	public void testHasReachedPeerLimitAtLimit() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		List<Peer> collectionMock = mock(List.class);

		when(torrentClientMock.getTorrentCount()).thenReturn(1);
		when(torrentMock.getPeers()).thenReturn(collectionMock);
		when(collectionMock.size()).thenReturn(5);

		EqualDistributor cut = new EqualDistributor(torrentClientMock, 5);
		assertTrue(cut.hasReachedPeerLimit(torrentMock), "Limit should have been reached (5 >= 5)");
	}

	@Test
	public void testHasReachedPeerLimitOverLimit() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		List<Peer> collectionMock = mock(List.class);

		when(torrentClientMock.getTorrentCount()).thenReturn(1);
		when(torrentMock.getPeers()).thenReturn(collectionMock);
		when(collectionMock.size()).thenReturn(6);

		EqualDistributor cut = new EqualDistributor(torrentClientMock, 5);
		assertTrue(cut.hasReachedPeerLimit(torrentMock), "Limit should have been reached (6 >= 5)");
	}

}
