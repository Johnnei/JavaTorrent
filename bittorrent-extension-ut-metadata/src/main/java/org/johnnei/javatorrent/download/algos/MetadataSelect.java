package org.johnnei.javatorrent.download.algos;

import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

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
