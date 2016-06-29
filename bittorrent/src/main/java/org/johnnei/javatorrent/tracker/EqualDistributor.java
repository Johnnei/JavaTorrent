package org.johnnei.javatorrent.tracker;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Torrent;

/**
 * An implementation of {@link IPeerDistributor} which attempts to equally distribute the peers.
 * Note that this implementation does not considered the given limit to be hard limit.
 * If the limit was reached and another torrent would get added this implementation will allow more peers to connect.
 * Also when the calculated limit is smaller than 1, the limit will be increased to 1.
 */
public class EqualDistributor implements IPeerDistributor {

	private final int globalLimit;

	private final TorrentClient torrentClient;

	/**
	 * Creates a distributor which attempts to equally distribute the peers over the torrents.
	 * @param torrentClient The torrent client for which this distributor is working.
	 * @param globalLimit The soft limit on the amount of connections.
	 */
	public EqualDistributor(TorrentClient torrentClient, int globalLimit) {
		this.torrentClient = torrentClient;
		this.globalLimit = globalLimit;
	}

	@Override
	public boolean hasReachedPeerLimit(Torrent torrent) {
		return torrent.getPeers().size() >= getLimitPerTorrent();
	}

	private int getLimitPerTorrent() {
		return Math.max(1, globalLimit / torrentClient.getTorrentCount());
	}
}
