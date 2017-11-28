package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link FullPieceSelect}
 */
public class FullPieceSelectTest {

	@Test
	public void testSelectStartedPiecesOverUnstarted() {
		Piece pieceOne = new Piece(null, new byte[20], 0, 10, 5);
		pieceOne.setBlockStatus(1, BlockStatus.Requested);
		Piece pieceTwo = new Piece(null, new byte[20], 1, 10, 5);

		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		when(filesMock.getNeededPieces()).thenReturn(Arrays.asList(pieceOne, pieceTwo).stream());

		Peer peerMock = mock(Peer.class);

		when(peerMock.hasPiece(anyInt())).thenReturn(true);

		Torrent torrentMock = mock(Torrent.class);
		when(torrentMock.getFileSet()).thenReturn(filesMock);

		FullPieceSelect cut = new FullPieceSelect(torrentMock);
		Optional<Piece> chosenPiece = cut.getPieceForPeer(peerMock);

		assertEquals(pieceOne, chosenPiece.get(), "Incorrect piece has been selected");
	}

	@Test
	public void testStateChangesDoNotAffectResult() {
		List<Piece> pieces = new ArrayList<>(300);
		for (int i = 0; i < 300; i++) {
			pieces.add(new Piece(null, new byte[20], i, 10, 5));
		}

		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		when(filesMock.getNeededPieces()).thenReturn(pieces.stream());

		Peer peerMock = mock(Peer.class);

		when(peerMock.hasPiece(anyInt())).thenReturn(true);

		Torrent torrentMock = mock(Torrent.class);
		when(torrentMock.getFileSet()).thenReturn(filesMock);
		when(torrentMock.getPeers()).thenReturn(Collections.singletonList(peerMock));

		FullPieceSelect cut = new FullPieceSelect(torrentMock);
		Optional<Piece> chosenPiece = cut.getPieceForPeer(peerMock);

		assertPresent("No piece has been selected, even though all are and all are equal.", chosenPiece);

		for (int i = 0; i < 300; i++) {
			// Once to detect which pieces the peer has and once to count the availability.
			verify(peerMock, times(2)).hasPiece(i);
		}
	}

	@Test
	public void testSelectStartedPiecesOverUnstartedExcludingPiecesWithoutAnyNeededBlock() {
		Piece pieceOne = new Piece(null, new byte[20], 0, 10, 5);
		pieceOne.setBlockStatus(0, BlockStatus.Requested);
		pieceOne.setBlockStatus(1, BlockStatus.Requested);
		Piece pieceTwo = new Piece(null, new byte[20], 1, 10, 5);

		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		when(filesMock.getNeededPieces()).thenReturn(Arrays.asList(pieceOne, pieceTwo).stream());

		Peer peerMock = mock(Peer.class);

		when(peerMock.hasPiece(anyInt())).thenReturn(true);

		Torrent torrentMock = mock(Torrent.class);
		when(torrentMock.getFileSet()).thenReturn(filesMock);

		FullPieceSelect cut = new FullPieceSelect(torrentMock);
		Optional<Piece> chosenPiece = cut.getPieceForPeer(peerMock);

		assertThat("Incorrect piece has been selected", assertPresent("A piece should be selected", chosenPiece), equalTo(pieceTwo));
	}

	@Test
	public void testSelectStartedPiecesOverUnstartedNonFirstElement() {
		Piece pieceOne = new Piece(null, new byte[20], 0, 10, 5);
		Piece pieceTwo = new Piece(null, new byte[20], 1, 10, 5);
		pieceTwo.setBlockStatus(1, BlockStatus.Requested);

		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		when(filesMock.getNeededPieces()).thenReturn(Arrays.asList(pieceOne, pieceTwo).stream());

		Peer peerMock = mock(Peer.class);

		when(peerMock.hasPiece(anyInt())).thenReturn(true);

		Torrent torrentMock = mock(Torrent.class);
		when(torrentMock.getFileSet()).thenReturn(filesMock);

		FullPieceSelect cut = new FullPieceSelect(torrentMock);
		Optional<Piece> chosenPiece = cut.getPieceForPeer(peerMock);

		assertThat("Incorrect piece has been selected", assertPresent("A piece should be selected", chosenPiece), equalTo(pieceTwo));
	}

	@Test
	public void testPickRarerPieces() {
		Piece pieceOne = new Piece(null, new byte[20], 0, 10, 5);
		Piece pieceTwo = new Piece(null, new byte[20], 1, 10, 5);

		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		when(filesMock.getNeededPieces()).thenReturn(Arrays.asList(pieceOne, pieceTwo).stream());

		Peer peerMock = mock(Peer.class);
		Peer peerTwoMock = mock(Peer.class);

		when(peerMock.hasPiece(anyInt())).thenReturn(true);
		when(peerTwoMock.hasPiece(eq(0))).thenReturn(true);
		when(peerTwoMock.hasPiece(eq(1))).thenReturn(false);

		Torrent torrentMock = mock(Torrent.class);
		when(torrentMock.getFileSet()).thenReturn(filesMock);
		when(torrentMock.getPeers()).thenReturn(Arrays.asList(peerMock, peerTwoMock));

		FullPieceSelect cut = new FullPieceSelect(torrentMock);
		Optional<Piece> chosenPiece = cut.getPieceForPeer(peerMock);

		assertThat("Incorrect piece has been selected", assertPresent("A piece should be selected.", chosenPiece), equalTo(pieceTwo));
	}
}
