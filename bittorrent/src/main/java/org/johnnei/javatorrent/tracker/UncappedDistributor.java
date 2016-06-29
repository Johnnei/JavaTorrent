package org.johnnei.javatorrent.tracker;

import java.util.function.Function;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Torrent;

/**
 * An implementation of {@link IPeerDistributor} which does not cap the amount of peers.
 */
public class UncappedDistributor implements IPeerDistributor {

	/**
	 * Creates an instance of {@link UncappedDistributor}
	 */
	public UncappedDistributor() {
		/* Add default constructor as torrent client is not required */
	}

	/**
	 * Creates an instance of {@link UncappedDistributor} which allows for <code>UncappedDistributor::new</code> to be used in
	 * {@link org.johnnei.javatorrent.TorrentClient.Builder#setPeerDistributor(Function)}
	 *
	 * @param tc The torrent client instance.
	 */
	public UncappedDistributor(TorrentClient tc) {
		/* Add constructor for usage in lambda syntax */
	}

	@Override
	public boolean hasReachedPeerLimit(Torrent torrent) {
		return false;
	}
}
