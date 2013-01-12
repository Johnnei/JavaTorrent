package torrent.download.algos;

import java.util.ArrayList;

import torrent.download.files.Piece;
import torrent.download.peer.Peer;

public interface IDownloadRegulator {

	/**
	 * Gets the name of this Download Regulator
	 * 
	 * @return The name
	 */
	public String getName();

	/**
	 * Selects the best peer for this Piece
	 * 
	 * @param p
	 *            The piece to check for
	 * @return The Peer which has to download this piece or <b>null</b> when no proper peer is found
	 */
	public ArrayList<Peer> getPeerForPiece(Piece p);

	/**
	 * Gets the next piece to download
	 * 
	 * @return The piece info of the next piece to download or <b>null</b> if no next piece is available
	 */
	public Piece getPiece();

}
