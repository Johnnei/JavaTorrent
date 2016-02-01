package org.johnnei.javatorrent.torrent.download.algos;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.files.Piece;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

/**
 * An basic improvement on {@link RandomSelect} RandomSelect.<br/>
 * Select a started piece, if none available: select the most available<br/>
 * Peer selection works the same as in {@link RandomSelect}
 *
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
	 * @return
	 */
	private Piece getMostAvailable() {
		List<Piece> undownloaded = torrent.getFiles().getNeededPieces().collect(Collectors.toList());
		int[] availability = new int[torrent.getFiles().getPieceCount()];
		int max = -1;
		Piece mostAvailable = null;
		for (Piece piece : undownloaded) {
			ArrayList<Peer> peers = torrent.getDownloadablePeers();
			for (Peer p : peers) {
				if (p.hasPiece(piece.getIndex())) {
					availability[piece.getIndex()]++;
				}
				if (availability[piece.getIndex()] > max) {
					if (piece.getRequestedCount() == 0) {
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
				filter(p -> p.isStarted() && p.getTotalRequestedCount() < p.getBlockCount()).
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
			for (int i = 0; i < undownloaded.size(); i++) {
				Piece piece = undownloaded.get(i);
				if (piece.getTotalRequestedCount() < piece.getBlockCount()) {
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
