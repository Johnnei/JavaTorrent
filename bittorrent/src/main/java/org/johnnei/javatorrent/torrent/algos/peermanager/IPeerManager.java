package org.johnnei.javatorrent.torrent.algos.peermanager;

public interface IPeerManager {

	/**
	 * The maximum amount of peers allowed
	 *
	 * @return
	 */
	public int getMaxPeers();

	/**
	 * The maximum amount of peers allowed including those which are still handshaking/connecting
	 *
	 * @return
	 */
	public int getMaxPendingPeers();

	/**
	 * Returns the amount of peers we want to recieve from the tracker
	 *
	 * @param connected The current amount of connected peers
	 * @return
	 */
	public int getAnnounceWantAmount(int connected);

	/**
	 * Gets the name of the Peer Manager
	 *
	 * @return
	 */
	public String getName();

}
