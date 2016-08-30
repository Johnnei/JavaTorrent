package org.johnnei.javatorrent.phases;

import java.util.Arrays;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PhaseSeed}
 */
public class PhaseSeedTest {

	private TorrentClient torrentClientMock = mock(TorrentClient.class);

	private Torrent torrentMock = mock(Torrent.class);

	private PhaseSeed cut;

	@Before
	public void setUp() {
		cut = new PhaseSeed(torrentClientMock, torrentMock);
	}

	@Test
	public void testProcess() throws Exception {
		Peer peerMockOne = mock(Peer.class);
		Peer peerMockTwo = mock(Peer.class);

		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		when(torrentMock.getPeers()).thenReturn(Arrays.asList(peerMockOne, peerMockTwo));
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getPieceCount()).thenReturn(2);

		// This leecher must not be removed
		when(peerMockOne.countHavePieces()).thenReturn(1);

		// This seeder must be removed and thus needs a socket to close.
		when(peerMockTwo.countHavePieces()).thenReturn(2);
		when(peerMockTwo.getBitTorrentSocket()).thenReturn(socketMock);

		cut.process();

		verify(socketMock).close();
	}

	@Test
	public void testIsDone() {
		assertFalse("This phase will never end", cut.isDone());
	}

	@Test
	public void testOnPhaseEnter() {
		cut.onPhaseEnter();

		// This phase should do nothing, assert that nothing happened.
		verifyNoMoreInteractions(torrentClientMock, torrentMock);
	}

	@Test
	public void testOnPhaseExit() {
		cut.onPhaseExit();

		// This phase should do nothing, assert that nothing happened.
		verifyNoMoreInteractions(torrentClientMock, torrentMock);
	}

	@Test
	public void testGetChokingStrategy() {
		assertNotNull("This method may never return null, even though won't care about what kind of strategy it returns.", cut.getChokingStrategy());
	}

}