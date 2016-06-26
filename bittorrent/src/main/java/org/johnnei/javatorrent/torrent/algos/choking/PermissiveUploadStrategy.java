package org.johnnei.javatorrent.torrent.algos.choking;

import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

/**
 * A choking strategy which doesn't care about us downloading and allows everyone to download from us.
 * This type of strategy is advised for {@link org.johnnei.javatorrent.phases.PhaseSeed} as it reduces overhead on checking choke states for downloading.
 *
 * @see PermissiveStrategy
 */
public class PermissiveUploadStrategy implements IChokingStrategy {

	@Override
	public void updateChoking(Peer peer) {
		boolean shouldBeChoked = !peer.isInterested(PeerDirection.Upload);
		if (peer.isChoked(PeerDirection.Upload) != shouldBeChoked) {
			peer.setChoked(PeerDirection.Upload, shouldBeChoked);
		}
	}
}
