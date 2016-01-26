package torrent.download.tracker;

import java.util.LinkedList;
import java.util.List;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.download.Torrent;
import torrent.download.peer.PeerConnectInfo;

public class PeerConnectorPool implements IPeerConnector {

	private List<PeerConnector> connectors;

	public PeerConnectorPool(TorrentClient torrentClient, TrackerManager manager) {
		connectors = new LinkedList<>();
		final int connectorCount = Config.getConfig().getInt("peer-max_concurrent_connecting");
		final int peerLimitPerConnector = Config.getConfig().getInt("peer-max_connecting") / connectorCount;

		for (int i = 0; i < connectorCount; i++) {
			PeerConnector connector = new PeerConnector(torrentClient, manager, peerLimitPerConnector);
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

	public int getFreeCapacity() {
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