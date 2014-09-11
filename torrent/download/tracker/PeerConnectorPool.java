package torrent.download.tracker;

import java.util.LinkedList;
import java.util.List;

import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.TorrentManager;
import torrent.download.Torrent;
import torrent.download.peer.Peer;

public class PeerConnectorPool {
	
	private List<PeerConnector> connectors;
	
	public PeerConnectorPool(TorrentManager manager) {
		connectors = new LinkedList<>();
		final int connectorCount = Config.getConfig().getInt("peer-max_concurrent_connecting");
		final int peerLimitPerConnector = Config.getConfig().getInt("peer-max_connecting") / connectorCount;
		
		for (int i = 0; i < connectorCount; i++) {
			PeerConnector connector = new PeerConnector(manager, peerLimitPerConnector);
			connectors.add(connector);
			Thread thread = new Thread(connector, String.format("Peer Connector #%d", i));
			thread.setDaemon(true);
			thread.start();
		}
	}
	
	/**
	 * Queues a peer to be connected
	 * @param p the peer to be connected
	 */
	public void addPeer(Peer peer) {
		PeerConnector connector = connectors.stream().min((a, b) -> a.getFreeCapacity() - b.getFreeCapacity()).get();
		
		if (connector.getFreeCapacity() == 0) {
			System.err.println("[PeerConnectorPool] Overflowing in peers. Can't distribute peers!");
			// TODO Implement a backlog of peers
			return;
		}
		
		connector.addPeer(peer);
		ThreadUtils.notify(connector.PEER_JOB_NOTIFY);
	}

	public int getConnectingCountFor(Torrent torrent) {
		return connectors.stream().mapToInt(c -> c.getConnectingCountFor(torrent)).sum();
	}
	

}