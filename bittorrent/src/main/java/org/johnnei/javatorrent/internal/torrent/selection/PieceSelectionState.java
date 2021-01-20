package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

public final class PieceSelectionState {

	private final Torrent torrent;

	private final Predicate<Peer> isRelevantPeer;

	private final Map<Piece, PieceRequestState> requestStates = new HashMap<>();

	private final Supplier<Optional<AbstractFileSet>> fileSetSupplier;

	public PieceSelectionState(Torrent torrent, Predicate<Peer> isRelevantPeer, Supplier<Optional<AbstractFileSet>> fileSetSupplier) {
		this.torrent = torrent;
		this.isRelevantPeer = isRelevantPeer;
		this.fileSetSupplier = fileSetSupplier;
	}

	public int getBlockQueueFor(Peer peer) {
		return requestStates.values().stream()
			.filter(s -> s.getAssociatedPeers().contains(peer))
			.mapToInt(s -> s.countPendingBlocksFor(peer))
			.sum();
	}

	public Torrent getTorrent() {
		return torrent;
	}

	public Predicate<Peer> getIsRelevantPeer() {
		return isRelevantPeer;
	}

	public Map<Piece, PieceRequestState> getRequestStates() {
		return requestStates;
	}

	public Optional<AbstractFileSet> getFileSet() {
		return fileSetSupplier.get();
	}
}
