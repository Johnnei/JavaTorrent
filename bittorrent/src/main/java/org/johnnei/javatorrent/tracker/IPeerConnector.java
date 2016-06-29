package org.johnnei.javatorrent.tracker;

import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.network.PeerConnectInfo;

/**
 * Interface which defines a system to establish connections with {@link PeerConnectInfo} received from
 * {@link org.johnnei.javatorrent.bittorrent.tracker.ITracker}. Implementation <strong>must</strong> honor the results {@link IPeerDistributor}
 */
public interface IPeerConnector {

	/**
	 * Queues a peer to be connected
	 * @param peer the peer to be connected
	 */
	void enqueuePeer(PeerConnectInfo peer);

	/**
	 * Starts the peer connector
	 */
	void start();

	/**
	 * Stops the peer connector.
	 */
	void stop();

	/**
	 * Gets the amount of peers are pending to be connected.
	 * @return The amount of peers which still need to be connected.
	 */
	int getConnectingCount();

	/**
	 * Calculates how many connections are assigned to the torrent but haven't passed the BitTorrent handshake yet.
	 * @param torrent The torrent for which connections must be counted.
	 * @return The amount of pending peers.
	 */
	int getConnectingCountFor(Torrent torrent);
}
