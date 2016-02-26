package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.List;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * A Piece selection algorithm which favors completing a started piece over starting a second piece.
 * @author Johnnei
 *
 */
public class FullPieceSelect implements IPieceSelector {

	private Torrent torrent;

	public FullPieceSelect(Torrent torrent) {
		this.torrent = torrent;
	}

	/**
	 * Gets the most available not started piece
	 *
	 * @return The most available {@link org.johnnei.javatorrent.torrent.files.BlockStatus#Needed} piece.
	 */
	private Piece getMostAvailable() {
		List<Piece> undownloaded = torrent.getFiles().getNeededPieces().collect(Collectors.toList());
		int[] availability = new int[torrent.getFiles().getPieceCount()];
		int max = -1;
		Piece mostAvailable = null;
		for (Piece piece : undownloaded) {
			for (Peer p : torrent.getRelevantPeers()) {
				if (p.hasPiece(piece.getIndex())) {
					availability[piece.getIndex()]++;
				}
				if (availability[piece.getIndex()] > max) {
					if (!piece.isStarted()) {
						max = availability[piece.getIndex()];
						mostAvailable = piece;
					}
				}
			}
		}
		if (mostAvailable == null) {
			return null;
		} else {
			return mostAvailable;
		}
	}

	@Override
	public Piece getPieceForPeer(Peer peer) {
		List<Piece> undownloaded = torrent.getFiles().getNeededPieces().collect(Collectors.toList());
		List<Piece> started = undownloaded.stream().
				filter(p -> p.isStarted() && p.hasBlockWithStatus(BlockStatus.Needed)).
				collect(Collectors.toList());

		// Check if peer has any of the started pieces
		for (Piece piece : started) {
			if (peer.hasPiece(piece.getIndex())) {
				return piece;
			}
		}
		// Peer doesn't have any of the started pieces (or there are no started pieces)
		Piece mostAvailable = getMostAvailable();
		if (mostAvailable != null && peer.hasPiece(mostAvailable.getIndex())) { // Try most available piece
			return mostAvailable;
		} else { // Nope, just request the first piece they have
			for (Piece piece : undownloaded) {
				if (piece.hasBlockWithStatus(BlockStatus.Needed)) {
					if (peer.hasPiece(piece.getIndex())) {
						return piece;
					}
				}
			}
			// This peer can't serve us any piece ):
			return null;
		}
	}

}
