package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static java.util.Collections.singletonList;
import static org.johnnei.javatorrent.test.DummyEntity.toPeerId;
import static org.johnnei.javatorrent.test.DummyEntity.withPeer;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PieceRequestStateTest {

	@Test
	public void testToString() {
		Peer peerOne = mock(Peer.class);
		when(peerOne.getIdAsString()).thenReturn("Peer#1");
		Peer peerTwo = mock(Peer.class);
		when(peerTwo.getIdAsString()).thenReturn("Peer#2");

		Block blockOne = new Block(1, 1);
		Block blockTwo = new Block(2, 1);
		Block blockThree = new Block(3, 1);

		PieceRequestState state = new PieceRequestState();
		state.addRequestedBlocks(peerTwo, singletonList(blockThree));
		state.addRequestedBlocks(peerOne, List.of(blockOne, blockTwo));

		assertEquals("PieceRequestState(requestedBlocks=[(Peer#1->[1,2]),(Peer#2->[3])])", state.toString());
	}

	@Test
	public void testEquals() {
		Peer peerOne = withPeer().setId(toPeerId("Peer#1")).build();
		Peer peerTwo = withPeer().setId(toPeerId("Peer#1")).build();

		Block blockOne = new Block(1, 1);
		Block blockTwo = new Block(1, 1);

		PieceRequestState stateOne = new PieceRequestState();
		stateOne.addRequestedBlocks(peerOne, singletonList(blockOne));

		PieceRequestState stateTwo = new PieceRequestState();
		stateTwo.addRequestedBlocks(peerTwo, singletonList(blockTwo));

		assertAll(
			() -> assertEquals(stateOne, stateOne),
			() -> assertEquals(stateTwo, stateTwo),
			() -> assertNotEquals(stateOne, null),
			() -> assertEquals(stateOne.hashCode(), stateTwo.hashCode()),
			() -> assertEquals(stateOne, stateTwo)
		);
	}
}
