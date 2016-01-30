package org.johnnei.javatorrent.torrent.download.tracker;

import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.PeerConnectInfo;

public interface IPeerConnector {

	/**
	 * Queues a peer to be connected
	 * @param p the peer to be connected
	 */
	void connectPeer(PeerConnectInfo peer);

	int getConnectingCountFor(Torrent torrent);

	/**
	 * Calculates how many peers can be accepted by {@link #connectPeer(PeerConnectInfo)} without discarding any.
	 * @return The amount of acceptable peers.
	 */
	int getAvailableCapacity();

}
