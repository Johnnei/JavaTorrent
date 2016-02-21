package org.johnnei.javatorrent.torrent.algos.choking;

import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * Strategy to handle the interested and choke states of {@link Peer}
 */
@FunctionalInterface
public interface IChokingStrategy {

	/**
	 * Checks and possibly updates the choke states of the Peer.
	 * @param peer The peer for which the states should be managed.
	 */
	void updateChoking(Peer peer);
}
