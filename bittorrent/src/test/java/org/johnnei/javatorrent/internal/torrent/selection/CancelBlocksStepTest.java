package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.Collections;
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

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CancelBlocksStepTest {

	private CancelBlocksStep cut = new CancelBlocksStep();

	@Test
	void testNoCancellationRequired() {
		Torrent torrent = mock(Torrent.class);
		AbstractFileSet fileSet = mock(AbstractFileSet.class);
		Piece piece = new Piece(null, null, 0, 1, 1);
		Block block = new Block(0, 1);
		Peer peer = mock(Peer.class);
		when(peer.getIdAsString()).thenReturn("Peer#1");
		List<Peer> peers = Collections.singletonList(peer);

		PieceSelectionState state = new PieceSelectionState(
			torrent,
			p -> true,
			() -> Optional.of(fileSet)
		);

		PieceRequestState initialState = new PieceRequestState();
		initialState.addRequestedBlocks(peer, Collections.singletonList(block));
		state.getRequestStates().put(piece, initialState);

		PieceRequestState expectedState = new PieceRequestState();
		expectedState.addRequestedBlocks(peer, Collections.singletonList(block));

		cut.cancelUnobtainableBlocks(state, peers, peers);

		assertAll(
			() -> assertTrue(state.getRequestStates().containsKey(piece)),
			() -> assertEquals(expectedState, state.getRequestStates().get(piece))
		);
	}

	@Test
	void testReturnBlocksOnPeerDisconnect() {
		Torrent torrent = mock(Torrent.class);
		AbstractFileSet fileSet = mock(AbstractFileSet.class);
		Piece piece = new Piece(null, null, 0, 1, 1);
		Block block = new Block(0, 1);
		block.setStatus(BlockStatus.Requested);
		Peer peer = mock(Peer.class);
		when(peer.getIdAsString()).thenReturn("Peer#1");

		PieceSelectionState state = new PieceSelectionState(
			torrent,
			p -> true,
			() -> Optional.of(fileSet)
		);

		PieceRequestState initialState = new PieceRequestState();
		initialState.addRequestedBlocks(peer, Collections.singletonList(block));
		state.getRequestStates().put(piece, initialState);

		PieceRequestState expectedState = new PieceRequestState();

		cut.cancelUnobtainableBlocks(state, Collections.emptyList(), Collections.emptyList());

		assertAll(
			() -> assertTrue(state.getRequestStates().containsKey(piece)),
			() -> assertEquals(expectedState, state.getRequestStates().get(piece)),
			() -> assertEquals(BlockStatus.Needed, block.getStatus())
		);
	}

	@Test
	void testReturnBlocksOnChokedPeer() {
		Torrent torrent = mock(Torrent.class);
		AbstractFileSet fileSet = mock(AbstractFileSet.class);
		when(fileSet.getBlockSize()).thenReturn(1);
		Piece piece = new Piece(null, null, 0, 1, 1);
		Block block = new Block(5, 1);
		block.setStatus(BlockStatus.Requested);
		Peer peer = mock(Peer.class);
		when(peer.getIdAsString()).thenReturn("Peer#1");

		PieceSelectionState state = new PieceSelectionState(
			torrent,
			p -> true,
			() -> Optional.of(fileSet)
		);

		PieceRequestState initialState = new PieceRequestState();
		initialState.addRequestedBlocks(peer, Collections.singletonList(block));
		state.getRequestStates().put(piece, initialState);

		PieceRequestState expectedState = new PieceRequestState();

		cut.cancelUnobtainableBlocks(state, Collections.singletonList(peer), Collections.emptyList());

		assertAll(
			() -> assertTrue(state.getRequestStates().containsKey(piece)),
			() -> assertEquals(expectedState, state.getRequestStates().get(piece)),
			() -> assertEquals(BlockStatus.Needed, block.getStatus()),
			() -> verify(peer).cancelBlockRequest(piece, 5, 1, PeerDirection.Download)
		);
	}

}
