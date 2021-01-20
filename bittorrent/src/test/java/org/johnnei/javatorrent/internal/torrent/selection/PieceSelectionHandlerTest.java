package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.pieceselector.PiecePrioritizer;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static java.util.Collections.singletonList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PieceSelectionHandlerTest {

	@Test
	public void testUpdateState() {
		CancelBlocksStep cleanStep = mock(CancelBlocksStep.class);
		RequestBlocksStep requestStep = mock(RequestBlocksStep.class);
		PiecePrioritizer prioritizer = mock(PiecePrioritizer.class);
		AbstractFileSet fileSet = mock(AbstractFileSet.class);
		Torrent torrent = mock(Torrent.class);

		Peer relevantPeer = mock(Peer.class);
		Peer irrelevantPeer = mock(Peer.class);

		when(relevantPeer.isChoked(PeerDirection.Download)).thenReturn(false);

		Piece pieceOne = new Piece(null, null, 0, 1, 1);
		Piece pieceTwo = new Piece(null, null, 1, 1, 1);

		List<Piece> pieces = List.of(pieceTwo, pieceOne);
		List<Piece> expectedPieces = List.of(pieceOne, pieceTwo);
		when(torrent.getPeers()).thenReturn(List.of(relevantPeer, irrelevantPeer));
		when(fileSet.getNeededPieces()).thenReturn(pieces.stream());
		when(prioritizer.sortByPriority(singletonList(relevantPeer), pieces)).thenReturn(expectedPieces);

		PieceSelectionState selectionState = new PieceSelectionState(torrent, p -> p == relevantPeer, () -> Optional.of(fileSet));
		PieceSelectionHandler cut = new PieceSelectionHandler(
			cleanStep, requestStep, prioritizer, () -> Optional.of(fileSet), selectionState
		);

		cut.updateState();

		verify(cleanStep).cancelUnobtainableBlocks(selectionState, List.of(relevantPeer, irrelevantPeer), singletonList(relevantPeer));
		verify(requestStep).requestBlocks(selectionState, singletonList(relevantPeer), expectedPieces);
	}

}
