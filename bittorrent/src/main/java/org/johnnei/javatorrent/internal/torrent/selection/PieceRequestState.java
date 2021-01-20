package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableMap;
import static java.util.Collections.unmodifiableSet;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;

public final class PieceRequestState {
	/**
	 * Peers that have/will receive block requests for this piece
	 */
	private final Set<Peer> associatedPeers = new HashSet<>();

	private final Map<Peer, Collection<Block>> requestedBlocks = new HashMap<>();

	public Collection<Block> disassociate(Peer peer) {
		associatedPeers.remove(peer);
		Collection<Block> blocks = requestedBlocks.remove(peer);
		if (blocks == null) {
			return emptyList();
		} else {
			return blocks;
		}
	}

	public void addRequestedBlocks(Peer peer, Collection<Block> blocks) {
		associatedPeers.add(peer);
		Collection<Block> allRequestedBlocks = requestedBlocks.getOrDefault(peer, new LinkedList<>());
		allRequestedBlocks.addAll(blocks);
		requestedBlocks.put(peer, allRequestedBlocks);
	}

	public int countPendingBlocksFor(Peer peer) {
		return (int) requestedBlocks.getOrDefault(peer, emptyList())
			.stream()
			.filter(b -> b.getStatus() == BlockStatus.Requested)
			.count();
	}

	public Set<Peer> getAssociatedPeers() {
		return unmodifiableSet(associatedPeers);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		PieceRequestState that = (PieceRequestState) o;
		return Objects.equals(requestedBlocks, that.requestedBlocks);
	}

	@Override
	public int hashCode() {
		return requestedBlocks.hashCode();
	}

	@Override
	public String toString() {
		return "PieceRequestState(requestedBlocks=" +
			requestedBlocks.entrySet().stream().sorted(comparing(e -> e.getKey().getIdAsString())).map(
				e -> {
					String blocks = e.getValue().stream().map(b -> Integer.toString(b.getIndex())).collect(joining(",", "[", "]"));
					return "(" + e.getKey().getIdAsString() + "->" + blocks + ")";
				}
			).collect(joining(",", "[", "]")) + ")";
	}
}
