package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.Collection;
import java.util.List;

import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * Sorter which takes a list (potentially sublist) of a Torrent's Pieces and sorts them by priority.
 */
public interface PiecePrioritizer {

	/**
	 * Sorts the given list of pieces by priority.
	 * @param peers The peers which are available to download from
	 * @param pieces The pieces to sort
	 * @return A list with the pieces sorted by priority. index 0 meaning highest priority, index n meaning lowest.
	 */
	List<Piece> sortByPriority(Collection<Peer> peers, List<Piece> pieces);

}
