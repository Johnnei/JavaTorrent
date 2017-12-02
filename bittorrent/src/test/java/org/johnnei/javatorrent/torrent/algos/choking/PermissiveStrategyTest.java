package org.johnnei.javatorrent.torrent.algos.choking;

import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PermissiveStrategy}
 */
public class PermissiveStrategyTest {

	@Test
	public void testUpdateChokingChoke() throws Exception {
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		Piece piece = new Piece(null, new byte[0], 1, 1, 1);

		when(torrentMock.getFileSet()).thenReturn(filesMock);
		when(filesMock.getNeededPieces()).thenReturn(Stream.of(piece));

		Peer peerMock = mock(Peer.class);
		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(peerMock.hasPiece(eq(1))).thenReturn(true);
		when(peerMock.isInterested(eq(PeerDirection.Download))).thenReturn(true);
		when(peerMock.isInterested(eq(PeerDirection.Upload))).thenReturn(false);
		when(peerMock.isChoked(eq(PeerDirection.Upload))).thenReturn(false);

		PermissiveStrategy cut = new PermissiveStrategy();
		cut.updateChoking(peerMock);

		verify(peerMock).setChoked(eq(PeerDirection.Upload), eq(true));
	}

	@Test
	public void testUpdateChokingUnchoke() throws Exception {
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		Piece piece = new Piece(null, new byte[0], 1, 1, 1);

		when(torrentMock.getFileSet()).thenReturn(filesMock);
		when(filesMock.getNeededPieces()).thenReturn(Stream.of(piece));

		Peer peerMock = mock(Peer.class);
		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(peerMock.hasPiece(eq(1))).thenReturn(true);
		when(peerMock.isInterested(eq(PeerDirection.Download))).thenReturn(true);
		when(peerMock.isInterested(eq(PeerDirection.Upload))).thenReturn(true);
		when(peerMock.isChoked(eq(PeerDirection.Upload))).thenReturn(true);

		PermissiveStrategy cut = new PermissiveStrategy();
		cut.updateChoking(peerMock);

		verify(peerMock).setChoked(eq(PeerDirection.Upload), eq(false));
	}

	@Test
	public void testUpdateChokingUpdateInterested() throws Exception {
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		Piece piece = new Piece(null, new byte[0], 1, 1, 1);

		when(torrentMock.getFileSet()).thenReturn(filesMock);
		when(filesMock.getNeededPieces()).thenReturn(Stream.of(piece));

		Peer peerMock = mock(Peer.class);
		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(peerMock.hasPiece(eq(1))).thenReturn(true);
		when(peerMock.isInterested(eq(PeerDirection.Download))).thenReturn(false);
		when(peerMock.isInterested(eq(PeerDirection.Upload))).thenReturn(false);
		when(peerMock.isChoked(eq(PeerDirection.Upload))).thenReturn(true);

		PermissiveStrategy cut = new PermissiveStrategy();
		cut.updateChoking(peerMock);

		verify(peerMock).setInterested(eq(PeerDirection.Download), eq(true));
	}
}
