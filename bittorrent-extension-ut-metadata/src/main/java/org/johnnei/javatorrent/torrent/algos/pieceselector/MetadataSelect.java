package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.Optional;

import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class MetadataSelect implements IPieceSelector {

	private Torrent torrent;

	public MetadataSelect(Torrent torrent) {
		this.torrent = torrent;
	}

	@Override
	public Optional<Piece> getPieceForPeer(Peer p) {
		Optional<MetadataFileSet> metadataFileSet = torrent.getMetadata();

		if (!metadataFileSet.isPresent()) {
			return Optional.empty();
		}

		Piece piece = metadataFileSet.get().getPiece(0);
		if (piece.hasBlockWithStatus(BlockStatus.Needed)) {
			return Optional.of(piece);
		}

		return Optional.empty();
	}

}
