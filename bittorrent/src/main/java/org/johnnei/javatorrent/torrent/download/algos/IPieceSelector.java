package org.johnnei.javatorrent.torrent.download.algos;

import org.johnnei.javatorrent.torrent.download.files.Piece;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

@FunctionalInterface
public interface IPieceSelector {

	/**
	 * Gets the next piece to download
	 *
	 * @return The piece info of the next piece to download or <b>null</b> if no next piece is available
	 */
	public Piece getPieceForPeer(Peer p);

}
