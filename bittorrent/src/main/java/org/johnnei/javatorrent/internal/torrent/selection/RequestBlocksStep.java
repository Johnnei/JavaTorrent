package org.johnnei.javatorrent.internal.torrent.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static java.util.stream.Collectors.toList;
import static org.johnnei.javatorrent.internal.utils.IterableOps.foldLeft;

public class RequestBlocksStep {

	public void requestBlocks(PieceSelectionState state, List<Peer> peers, List<Piece> pieces) {
		cleanUpState(state, pieces);
		peers.forEach(peer -> requestBlocksForPeer(state, peer, pieces));
	}

	private void cleanUpState(PieceSelectionState selectionState, List<Piece> pieces) {
		List<Piece> piecesToCleanUp = selectionState.getRequestStates().keySet()
			.stream()
			.filter(piece -> !pieces.contains(piece))
			.collect(toList());
		piecesToCleanUp.forEach(piece -> selectionState.getRequestStates().remove(piece));
	}

	private void requestBlocksForPeer(PieceSelectionState state, Peer peer, List<Piece> pieces) {
		state.getFileSet().ifPresent(fileSet -> {
			List<Piece> hasPieces = pieces.stream().filter(piece -> fileSet.hasPiece(peer, piece.getIndex())).collect(toList());
			int maxBlocks = peer.getRequestLimit() - state.getBlockQueueFor(peer);
			List<PieceBlocks> blocksToRequest = foldLeft(hasPieces, new ArrayList<>(), (blocks, piece) -> {
				int remainingBlocks = maxBlocks - blocks.stream().mapToInt(pb -> pb.blocks.size()).sum();

				if (remainingBlocks > 0) {
					Collection<Block> requestableBlocks = piece.getRequestableBlocks(remainingBlocks);
					blocks.add(new PieceBlocks(piece, requestableBlocks));
				}

				return blocks;
			});

			blocksToRequest.forEach(pb -> {
				Piece piece = pb.piece;
				Collection<Block> requestedBlocks = pb.blocks.stream()
					.filter(block ->
						peer.addBlockRequest(
							piece,
							fileSet.getBlockSize() * block.getIndex(),
							block.getSize(),
							PeerDirection.Download)
					).collect(toList());

				requestedBlocks.forEach(block -> block.setStatus(BlockStatus.Requested));
				PieceRequestState requestState = state.getRequestStates().computeIfAbsent(piece, ignored -> new PieceRequestState());
				requestState.addRequestedBlocks(peer, requestedBlocks);
			});
		});
	}

	private static class PieceBlocks {

		private final Piece piece;

		private final Collection<Block> blocks;

		public PieceBlocks(Piece piece, Collection<Block> blocks) {
			this.piece = piece;
			this.blocks = blocks;
		}
	}
}
