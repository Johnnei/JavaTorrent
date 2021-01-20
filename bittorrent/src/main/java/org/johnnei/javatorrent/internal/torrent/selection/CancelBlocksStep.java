package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static java.util.stream.Collectors.toList;

/**
 * Piece Selection step that returns blocks back into the pending blocks pool.
 * This includes:
 * - Blocks that were requested from peers that are now disconnected
 * - Blocks that were requested from peers that have now choked us
 */
public class CancelBlocksStep {

	public void cancelUnobtainableBlocks(PieceSelectionState state, List<Peer> allPeers, List<Peer> relevantPeers) {
		List<Peer> noLongerRelevantPeers = allPeers.stream()
			.filter(peer -> !relevantPeers.contains(peer))
			.collect(toList());

		List<Peer> disconnectedPeers = state.getRequestStates().values().stream()
			.flatMap(requestState -> requestState.getAssociatedPeers().stream())
			.distinct()
			.filter(peer -> !relevantPeers.contains(peer))
			.collect(toList());

		state.getFileSet().ifPresent(fileSet -> {
			for (Map.Entry<Piece, PieceRequestState> entry : state.getRequestStates().entrySet()) {
				for (Peer peer : noLongerRelevantPeers) {
					Collection<Block> blocks = entry.getValue().disassociate(peer);
					blocks.forEach(block -> {
						block.setStatus(BlockStatus.Needed);
						peer.cancelBlockRequest(
							entry.getKey(),
							fileSet.getBlockSize() * block.getIndex(),
							block.getSize(),
							PeerDirection.Download
						);
					});
				}

				for (Peer peer : disconnectedPeers) {
					entry.getValue().disassociate(peer).forEach(block -> block.setStatus(BlockStatus.Needed));
				}
			}
		});
	}
}
