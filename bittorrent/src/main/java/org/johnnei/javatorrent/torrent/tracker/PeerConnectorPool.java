package org.johnnei.javatorrent.torrent.tracker;

import java.util.LinkedList;
import java.util.List;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.PeerConnectInfo;
import org.johnnei.javatorrent.utils.ThreadUtils;
import org.johnnei.javatorrent.utils.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerConnectorPool implements IPeerConnector {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnectorPool.class);

	private List<PeerConnector> connectors;

	public PeerConnectorPool(TorrentClient torrentClient) {
		connectors = new LinkedList<>();
		final int connectorCount = Config.getConfig().getInt("peer-max_concurrent_connecting");
		final int peerLimitPerConnector = Config.getConfig().getInt("peer-max_connecting") / connectorCount;

		LOGGER.info(
				String.format("Starting PeerConnector pool with %d connectors with a queue limit of %d peers each.", connectorCount, peerLimitPerConnector));
		for (int i = 0; i < connectorCount; i++) {
			LOGGER.trace(String.format("Starting PeerConnector thread #%d", i));
			PeerConnector connector = new PeerConnector(torrentClient, peerLimitPerConnector);
			connectors.add(connector);
			Thread thread = new Thread(connector, String.format("Peer Connector #%d", i));
			thread.setDaemon(true);
			thread.start();
		}
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.IPeerConnector#connectPeer(torrent.download.peer.PeerConnectInfo)
	 */
	@Override
	public void connectPeer(PeerConnectInfo peer) {
		PeerConnector connector = connectors.stream().max((a, b) -> a.getFreeCapacity() - b.getFreeCapacity()).get();

		if (connector.getFreeCapacity() == 0) {
			System.err.println("[PeerConnectorPool] Overflowing in peers. Can't distribute peers!");
			// TODO Implement a backlog of peers
			return;
		}

		connector.addPeer(peer);
		ThreadUtils.notify(connector.PEER_JOB_NOTIFY);
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.IPeerConnector#getAvailableCapacity()
	 */
	@Override
	public int getAvailableCapacity() {
		return connectors.stream().mapToInt(PeerConnector::getFreeCapacity).sum();
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.IPeerConnector#getConnectingCountFor(torrent.download.Torrent)
	 */
	@Override
	public int getConnectingCountFor(Torrent torrent) {
		return connectors.stream().mapToInt(c -> c.getConnectingCountFor(torrent)).sum();
	}


}