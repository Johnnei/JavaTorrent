package org.johnnei.javatorrent.torrent.algos.choking;

import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

/**
 * A simple choking strategy which attempt to get unchoked by everyone who has any piece we don't have and unchokes
 * anyone who's interested in us.
 */
public class PermissiveStrategy extends PermissiveUploadStrategy {

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateChoking(Peer peer) {
		boolean interested = peer.getTorrent().getFileSet().getNeededPieces().anyMatch(piece -> peer.hasPiece(piece.getIndex()));

		if (peer.isInterested(PeerDirection.Download) != interested) {
			peer.setInterested(PeerDirection.Download, interested);
		}

		super.updateChoking(peer);
	}
}
