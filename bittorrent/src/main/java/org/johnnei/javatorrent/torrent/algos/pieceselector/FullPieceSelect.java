package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.Optional;

import org.johnnei.javatorrent.torrent.Torrent;
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
		return (int) torrent.getRelevantPeers().stream().filter(peer -> peer.hasPiece(piece.getIndex())).count();
	}

	/**
	 * Note: this comparator imposes orderings that are inconsistent with equals.
	 */
	private int comparePieces(Piece a, Piece b) {
		if (a.isStarted() == b.isStarted()) {
			// When they are either both started or not, the availability indicates the priority.
			return countAvailability(a) - countAvailability(b);
		}

		if (a.isStarted() && !b.isStarted()) {
			return -1;
		} else {
			return 1;
		}
	}

	@Override
	public Optional<Piece> getPieceForPeer(Peer peer) {
		return torrent.getFiles().getNeededPieces()
				.filter(piece -> peer.hasPiece(piece.getIndex()))
				.sorted(this::comparePieces)
				.findFirst();
	}

}
