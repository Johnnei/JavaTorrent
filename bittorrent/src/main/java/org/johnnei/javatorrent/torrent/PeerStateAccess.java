package org.johnnei.javatorrent.torrent;

import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

/**
 * Contract to give access to details of the internal torrent state.
 */
public interface PeerStateAccess {

	int getPendingBlocks(Peer peer, PeerDirection direction);

}
