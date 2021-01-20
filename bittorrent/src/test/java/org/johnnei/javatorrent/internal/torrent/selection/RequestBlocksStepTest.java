package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestBlocksStepTest {

	private RequestBlocksStep cut = new RequestBlocksStep();

	@Test
	public void testAssignBlockToPeer() {
		Torrent torrent = mock(Torrent.class);
		Peer peer = mock(Peer.class);
		when(peer.getIdAsString()).thenReturn("Peer#1");
		when(peer.getRequestLimit()).thenReturn(1);
		AbstractFileSet fileSet = mock(AbstractFileSet.class);
		when(fileSet.getBlockSize()).thenReturn(1);
		Piece piece = new Piece(null, null, 1, 1, 1);

		PieceSelectionState state = new PieceSelectionState(torrent, p -> true, () -> Optional.of(fileSet));

		when(fileSet.hasPiece(peer, 1)).thenReturn(true);
		when(peer.addBlockRequest(piece, 0, 1, PeerDirection.Download)).thenReturn(true);

		cut.requestBlocks(state, singletonList(peer), singletonList(piece));

		PieceRequestState expectedState = new PieceRequestState();
		Block expectedBlock = new Block(0, 1);
		expectedBlock.setStatus(BlockStatus.Requested);
		expectedState.addRequestedBlocks(peer, singletonList(expectedBlock));

		assertEquals(expectedState, state.getRequestStates().get(piece));
	}

	@Test
	public void testRespectRequestLimitFromPeer() {
		Torrent torrent = mock(Torrent.class);
		Peer peer = mock(Peer.class);
		when(peer.getIdAsString()).thenReturn("Peer#1");
		when(peer.getRequestLimit()).thenReturn(2);
		AbstractFileSet fileSet = mock(AbstractFileSet.class);
		when(fileSet.getBlockSize()).thenReturn(1);
		Piece piece = new Piece(null, null, 1, 3, 1);

		PieceSelectionState state = new PieceSelectionState(torrent, p -> true, () -> Optional.of(fileSet));

		when(fileSet.hasPiece(peer, 1)).thenReturn(true);
		when(peer.addBlockRequest(piece, 0, 1, PeerDirection.Download)).thenReturn(true);
		when(peer.addBlockRequest(piece, 1, 1, PeerDirection.Download)).thenReturn(true);

		cut.requestBlocks(state, singletonList(peer), singletonList(piece));

		PieceRequestState expectedState = new PieceRequestState();
		Block expectedBlock = new Block(0, 1);
		expectedBlock.setStatus(BlockStatus.Requested);
		Block expectedBlock2 = new Block(1, 1);
		expectedBlock2.setStatus(BlockStatus.Requested);
		expectedState.addRequestedBlocks(peer, List.of(expectedBlock, expectedBlock2));

		assertEquals(expectedState, state.getRequestStates().get(piece));
	}

	@Test
	public void testRemovesCompletedPiecesFromState() {
		Torrent torrent = mock(Torrent.class);
		Peer peer = mock(Peer.class);
		when(peer.getIdAsString()).thenReturn("Peer#1");
		AbstractFileSet fileSet = mock(AbstractFileSet.class);
		when(fileSet.getBlockSize()).thenReturn(1);
		Piece piece = new Piece(null, null, 1, 1, 1);
		Piece completedPiece = new Piece(null, null, 2, 1, 1);
		completedPiece.setBlockStatus(0, BlockStatus.Verified);

		PieceSelectionState state = new PieceSelectionState(torrent, p -> true, () -> Optional.of(fileSet));
		state.getRequestStates().put(piece, new PieceRequestState());
		state.getRequestStates().put(completedPiece, new PieceRequestState());

		cut.requestBlocks(state, singletonList(peer), singletonList(piece));

		assertAll(
			() -> assertEquals(new PieceRequestState(), state.getRequestStates().get(piece)),
			() -> assertNull(state.getRequestStates().get(completedPiece))
		);
	}

}
