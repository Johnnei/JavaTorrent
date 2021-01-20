package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.Collection;
import java.util.List;

import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class NopPrioritizer implements PiecePrioritizer {

	@Override
	public List<Piece> sortByPriority(Collection<Peer> peers, List<Piece> pieces) {
		return pieces;
	}

}
