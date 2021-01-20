package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class AvailabilityPrioritizer implements PiecePrioritizer {

	@Override
	public List<Piece> sortByPriority(Collection<Peer> peers, List<Piece> pieces) {
		List<Piece> sorted = new ArrayList<>(pieces);
		Map<Piece, Integer> memoizedAvailability = new HashMap<>();

		sorted.sort((a, b) -> {
			int availabilityA = memoizedAvailability.computeIfAbsent(a, (key) -> countAvailability(peers, key));
			int availabilityB = memoizedAvailability.computeIfAbsent(b, (key) -> countAvailability(peers, key));

			return Integer.compare(availabilityA, availabilityB);
		});

		return sorted;
	}

	private int countAvailability(Collection<Peer> peers, Piece piece) {
		return (int) peers.stream().filter(peer -> peer.hasPiece(piece.getIndex())).count();
	}
}
