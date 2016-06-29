package org.johnnei.javatorrent.tracker;

import org.johnnei.javatorrent.torrent.Torrent;

/**
 * An interface which defines how peers get distributed over all the torrents within {@link org.johnnei.javatorrent.TorrentClient}
 */
@FunctionalInterface
public interface IPeerDistributor {

	/**
	 * @param torrent The torrent to calculate the limit for.
	 * @return <code>true</code> when the torrent has reached the calculated peer count limit.
	 */
	boolean hasReachedPeerLimit(Torrent torrent);

}
