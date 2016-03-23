package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.Arrays;
import java.util.Optional;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link FullPieceSelect}
 */
public class FullPieceSelectTest extends EasyMockSupport {

	@Test
	public void testSelectStartedPiecesOverUnstarted() {
		Piece pieceOne = new Piece(null, new byte[20], 0, 10, 5);
		pieceOne.setBlockStatus(1, BlockStatus.Requested);
		Piece pieceTwo = new Piece(null, new byte[20], 1, 10, 5);

		AbstractFileSet filesMock = createMock(AbstractFileSet.class);
		expect(filesMock.getNeededPieces()).andReturn(Arrays.asList(pieceOne, pieceTwo).stream());

		Peer peerMock = createMock(Peer.class);

		expect(peerMock.hasPiece(anyInt())).andReturn(true).atLeastOnce();

		Torrent torrentMock = createMock(Torrent.class);
		expect(torrentMock.getFileSet()).andStubReturn(filesMock);

		replayAll();

		FullPieceSelect cut = new FullPieceSelect(torrentMock);
		Optional<Piece> chosenPiece = cut.getPieceForPeer(peerMock);

		verifyAll();
		assertEquals("Incorrect piece has been selected", pieceOne, chosenPiece.get());
	}

	@Test
	public void testSelectStartedPiecesOverUnstartedNonFirstElement() {
		Piece pieceOne = new Piece(null, new byte[20], 0, 10, 5);
		Piece pieceTwo = new Piece(null, new byte[20], 1, 10, 5);
		pieceTwo.setBlockStatus(1, BlockStatus.Requested);

		AbstractFileSet filesMock = createMock(AbstractFileSet.class);
		expect(filesMock.getNeededPieces()).andReturn(Arrays.asList(pieceOne, pieceTwo).stream());

		Peer peerMock = createMock(Peer.class);

		expect(peerMock.hasPiece(anyInt())).andReturn(true).atLeastOnce();

		Torrent torrentMock = createMock(Torrent.class);
		expect(torrentMock.getFileSet()).andStubReturn(filesMock);

		replayAll();

		FullPieceSelect cut = new FullPieceSelect(torrentMock);
		Optional<Piece> chosenPiece = cut.getPieceForPeer(peerMock);

		verifyAll();
		assertEquals("Incorrect piece has been selected", pieceTwo, chosenPiece.get());
	}

	@Test
	public void testPickRarerPieces() {
		Piece pieceOne = new Piece(null, new byte[20], 0, 10, 5);
		Piece pieceTwo = new Piece(null, new byte[20], 1, 10, 5);

		AbstractFileSet filesMock = createMock(AbstractFileSet.class);
		expect(filesMock.getNeededPieces()).andReturn(Arrays.asList(pieceOne, pieceTwo).stream());

		Peer peerMock = createMock(Peer.class);
		Peer peerTwoMock = createMock(Peer.class);

		expect(peerMock.hasPiece(anyInt())).andReturn(true).atLeastOnce();
		expect(peerTwoMock.hasPiece(eq(0))).andReturn(true).atLeastOnce();
		expect(peerTwoMock.hasPiece(eq(1))).andReturn(false).atLeastOnce();

		Torrent torrentMock = createMock(Torrent.class);
		expect(torrentMock.getFileSet()).andStubReturn(filesMock);
		expect(torrentMock.getRelevantPeers()).andStubReturn(Arrays.asList(peerMock, peerTwoMock));

		replayAll();

		FullPieceSelect cut = new FullPieceSelect(torrentMock);
		Optional<Piece> chosenPiece = cut.getPieceForPeer(peerMock);

		verifyAll();
		assertEquals("Incorrect piece has been selected", pieceTwo, chosenPiece.get());
	}
}