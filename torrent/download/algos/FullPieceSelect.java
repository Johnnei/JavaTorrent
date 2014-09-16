package torrent.download.algos;

import java.util.ArrayList;

import torrent.download.Torrent;
import torrent.download.files.Piece;
import torrent.download.peer.Peer;

/**
 * An basic improvement on {@link RandomSelect} RandomSelect.<br/>
 * Select a started piece, if none available: select the most available<br/>
 * Peer selection works the same as in {@link RandomSelect}
 * 
 * @author Johnnei
 * 
 */
public class FullPieceSelect implements IDownloadRegulator {

	private Torrent torrent;

	public FullPieceSelect(Torrent torrent) {
		this.torrent = torrent;
	}

	@Override
	public String getName() {
		return "Full Pieces first pass";
	}

	/**
	 * Gets the most available not started piece
	 * 
	 * @return
	 */
	private Piece getMostAvailable() {
		ArrayList<Piece> undownloaded = torrent.getFiles().getNeededPieces();
		int[] availability = new int[torrent.getFiles().getPieceCount()];
		int max = -1;
		Piece mostAvailable = null;
		for (Piece piece : undownloaded) {
			ArrayList<Peer> peers = torrent.getDownloadablePeers();
			for (Peer p : peers) {
				if (p.hasPiece(piece.getIndex()))
					availability[piece.getIndex()]++;
				if (availability[piece.getIndex()] > max) {
					if (piece.getRequestedCount() == 0) {
						max = availability[piece.getIndex()];
						mostAvailable = piece;
					}
				}
			}
		}
		if (mostAvailable == null)
			return null;
		else {
			return mostAvailable;
		}
	}

	@Override
	public Piece getPieceForPeer(Peer peer) {
		ArrayList<Piece> undownloaded = torrent.getFiles().getNeededPieces();
		ArrayList<Piece> started = new ArrayList<Piece>();
		for (int i = 0; i < undownloaded.size(); i++) {
			Piece piece = undownloaded.get(i);
			if (piece.isStarted() && piece.getTotalRequestedCount() < piece.getBlockCount()) {
				started.add(piece);
			}
		}
		// Check if peer has any of the started pieces
		for (int i = 0; i < started.size(); i++) {
			Piece piece = started.get(i);
			if (peer.getTorrent().getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA) {
				return piece;
			}
			if (peer.hasPiece(piece.getIndex())) {
				return piece;
			}
		}
		// Peer doesn't have any of the started pieces (or there are no started pieces)
		Piece mostAvailable = getMostAvailable();
		if (mostAvailable != null && peer.getTorrent().getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA)
			return mostAvailable;
		if (mostAvailable != null && peer.hasPiece(mostAvailable.getIndex())) { // Try most available piece
			return mostAvailable;
		} else { // Nope, just request the first piece they have
			for (int i = 0; i < undownloaded.size(); i++) {
				Piece piece = undownloaded.get(i);
				if (piece.getTotalRequestedCount() < piece.getBlockCount()) {
					if (peer.getTorrent().getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA)
						return piece;
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
