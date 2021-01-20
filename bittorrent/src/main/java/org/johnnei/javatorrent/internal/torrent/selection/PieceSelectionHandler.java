package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.algos.pieceselector.PiecePrioritizer;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static java.util.stream.Collectors.toList;

public class PieceSelectionHandler {

	private final CancelBlocksStep cancelBlocksStep;

	private final RequestBlocksStep requestBlocksStep;

	private final PiecePrioritizer piecePrioritizer;

	private final PieceSelectionState state;

	private final Supplier<Optional<AbstractFileSet>> fileSetSupplier;

	public PieceSelectionHandler(Supplier<Optional<AbstractFileSet>> fileSetSupplier, PiecePrioritizer piecePrioritizer, PieceSelectionState state) {
		this(new CancelBlocksStep(), new RequestBlocksStep(), piecePrioritizer, fileSetSupplier, state);
	}

	public PieceSelectionHandler(CancelBlocksStep cancelBlocksStep, RequestBlocksStep requestBlocksStep, PiecePrioritizer piecePrioritizer, Supplier<Optional<AbstractFileSet>> fileSetSupplier, PieceSelectionState state) {
		this.cancelBlocksStep = cancelBlocksStep;
		this.requestBlocksStep = requestBlocksStep;
		this.piecePrioritizer = piecePrioritizer;
		this.fileSetSupplier = fileSetSupplier;
		this.state = state;
	}

	public void updateState() {
		fileSetSupplier.get().ifPresent(fileSet -> {
			List<Peer> allPeers = state.getTorrent().getPeers();
			List<Peer> peers = allPeers.stream()
				.filter(peer -> !peer.isChoked(PeerDirection.Download))
				.filter(state.getIsRelevantPeer())
				.collect(toList());

			List<Piece> pendingPieces = piecePrioritizer.sortByPriority(
				peers,
				fileSet.getNeededPieces().collect(toList())
			);

			cancelBlocksStep.cancelUnobtainableBlocks(state, allPeers, peers);
			requestBlocksStep.requestBlocks(state, peers, pendingPieces);
		});
	}

	public int getBlockQueueFor(Peer peer) {
		return state.getBlockQueueFor(peer);
	}

}
