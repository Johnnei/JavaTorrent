package org.johnnei.javatorrent.tracker;

import java.util.LinkedList;
import java.util.List;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.torrent.Torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerConnectorPool implements IPeerConnector {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnectorPool.class);

	private List<IPeerConnector> connectors;

	/**
	 * Creates a new pool of {@link PeerConnector}s.
	 * @param torrentClient The torrent client for which the peers will be connecoted.
	 * @param maxConcurrentConnecting The amount of concurrent connecting peers.
	 */
	public PeerConnectorPool(TorrentClient torrentClient, int maxConcurrentConnecting) {
		connectors = new LinkedList<>();

		LOGGER.info("Starting PeerConnector pool with {} connectors.", maxConcurrentConnecting);
		for (int i = 0; i < maxConcurrentConnecting; i++) {
			connectors.add(new PeerConnector(torrentClient));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() {
		connectors.forEach(IPeerConnector::start);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		connectors.forEach(IPeerConnector::stop);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void enqueuePeer(PeerConnectInfo peer) {
		IPeerConnector connector = connectors.stream().min((a, b) -> a.getConnectingCount() - b.getConnectingCount()).get();
		connector.enqueuePeer(peer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getConnectingCount() {
		return connectors.stream().mapToInt(IPeerConnector::getConnectingCount).sum();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getConnectingCountFor(Torrent torrent) {
		return connectors.stream().mapToInt(c -> c.getConnectingCountFor(torrent)).sum();
	}


}