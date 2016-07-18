package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.Optional;

import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * A Piece selection algorithm which favors completing a started piece over starting a second piece.
 * This selector also favors rarer pieces over highly available ones (as advised by BEP #3).
 * @author Johnnei
 *
 */
public class FullPieceSelect implements IPieceSelector {

	private Torrent torrent;

	public FullPieceSelect(Torrent torrent) {
		this.torrent = torrent;
	}

	private int countAvailability(Piece piece) {
		return (int) torrent.getPeers().stream()
				.filter(peer -> peer.hasPiece(piece.getIndex()))
				.count();
	}

	/**
	 * Note: this comparator imposes orderings that are inconsistent with equals.
	 */
	private int comparePieces(CachedPiece a, CachedPiece b) {
		if (a.isStarted() == b.isStarted()) {
			// When they are either both started or not, the availability indicates the priority.
			return a.getAvailability() - b.getAvailability();
		}

		if (a.isStarted() && !b.isStarted()) {
			return -1;
		} else {
			return 1;
		}
	}

	@Override
	public Optional<Piece> getPieceForPeer(Peer peer) {
		return torrent.getFileSet().getNeededPieces()
				.filter(piece -> piece.hasBlockWithStatus(BlockStatus.Needed))
				.filter(piece -> peer.hasPiece(piece.getIndex()))
				// Create a cache of the information used in the comparator to create a consistent sorting state.
				.map(piece -> new CachedPiece(piece, countAvailability(piece)))
				.sorted(this::comparePieces)
				// Retrieve the actual piece from the cache.
				.map(CachedPiece::getPiece)
				.findFirst();
	}

	private class CachedPiece {

		private final Piece piece;

		private final boolean isStarted;

		private final int availability;

		CachedPiece(Piece piece, int availability) {
			this.piece = piece;
			this.availability = availability;
			isStarted = piece.isStarted();
		}

		Piece getPiece() {
			return piece;
		}

		boolean isStarted() {
			return isStarted;
		}

		int getAvailability() {
			return availability;
		}
	}

}
