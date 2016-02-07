package org.johnnei.javatorrent.torrent.download.algos;

import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.files.Piece;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

public class MetadataSelect implements IPieceSelector {

	private Torrent torrent;

	public MetadataSelect(Torrent torrent) {
		this.torrent = torrent;
	}

	@Override
	public Piece getPieceForPeer(Peer p) {
		Piece piece = torrent.getFiles().getPiece(0);
		if (piece.getRequestedCount() < piece.getBlockCount()) {
			return piece;
		}

		return null;
	}

}
