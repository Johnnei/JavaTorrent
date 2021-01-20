package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class PieceSelectionStateTest {

	private Block newBlock(int index) {
		Block b = new Block(index, 1);
		b.setStatus(BlockStatus.Requested);
		return b;
	}

	@Test
	void testGetBlockQueueFor() {
		Torrent torrent = mock(Torrent.class);

		PieceSelectionState cut = new PieceSelectionState(torrent, p -> true, Optional::empty);

		Piece pieceOne = new Piece(null, null, 0, 10, 1);
		Piece pieceTwo = new Piece(null, null, 0, 10, 1);

		Peer peerOne = mock(Peer.class);
		Peer peerTwo = mock(Peer.class);

		Block block2 = newBlock(2);
		block2.setStatus(BlockStatus.Stored);

		cut.getRequestStates().put(pieceOne, new PieceRequestState());
		cut.getRequestStates().put(pieceTwo, new PieceRequestState());
		cut.getRequestStates().get(pieceOne).addRequestedBlocks(peerOne, List.of(newBlock(0), block2, newBlock(3)));
		cut.getRequestStates().get(pieceOne).addRequestedBlocks(peerTwo, List.of(newBlock(1)));
		cut.getRequestStates().get(pieceTwo).addRequestedBlocks(peerTwo, List.of(newBlock(0), newBlock(1)));

		assertAll(
			() -> assertEquals(2, cut.getBlockQueueFor(peerOne)),
			() -> assertEquals(3, cut.getBlockQueueFor(peerTwo))
		);
	}
}
