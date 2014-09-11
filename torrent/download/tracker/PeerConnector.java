package torrent.download.tracker;

import java.io.IOException;
import java.util.LinkedList;

import org.johnnei.utils.ThreadUtils;

import torrent.TorrentManager;
import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.encoding.SHA1;
import torrent.protocol.BitTorrentHandshake;
import torrent.protocol.BitTorrentUtil;

public class PeerConnector implements Runnable {

	private final Object LOCK_PEER_LIST = new Object();
	
	/**
	 * The object on which the thread will start sleeping once it is out of work
	 */
	public final Object PEER_JOB_NOTIFY = new Object();
	
	/**
	 * List of peer that are currently being connected
	 */
	private LinkedList<Peer> peers;
	
	private TorrentManager manager;
	
	private final int maxPeers;
	
	public PeerConnector(TorrentManager manager, int maxConnecting) {
		this.manager = manager;
		this.maxPeers = maxConnecting;
		peers = new LinkedList<>();
	}

	/**
	 * Adds a pending connection peer to the connection cycle
	 * 
	 * @param peer The peer to connect
	 */
	public void addPeer(Peer peer) {
		if (peers.size() >= maxPeers) {
			throw new IllegalStateException(String.format("Connector is full"));
		}
		
		synchronized (LOCK_PEER_LIST) {
			peers.add(peer);
		}
	}

	public void run() {
		while (true) {
			
			if (peers.isEmpty()) {
				ThreadUtils.wait(PEER_JOB_NOTIFY);
			}
			
			Peer peer = null;
			
			synchronized (LOCK_PEER_LIST) {
				peer = peers.remove();
			}
			
			if (peer == null) {
				continue;
			}
			
			try {
				peer.connect();
				
				if (peer.closed()) {
					continue;
				}
				
				peer.sendHandshake(manager.getTrackerManager().getPeerId());
				
				long timeWaited = 0;
				while (!peer.canReadMessage() && timeWaited < 10_000) {
					final int INTERVAL = 100;
					ThreadUtils.sleep(INTERVAL);
					timeWaited += INTERVAL;
				}
				
				if (!peer.canReadMessage()) {
					throw new IOException("Handshake timeout");
				}
				
				if (!checkHandshake(peer)) {
					throw new IOException("Unexpected/Incorrect torrent hash received");
				}
				
				BitTorrentUtil.onPostHandshake(peer);
				
				
			} catch (IOException e) {
				System.err.println(String.format("[PeerConnector] Failed to connect peer: %s", e.getMessage()));
				peer.close();
			}
		}
	}
	
	private boolean checkHandshake(Peer peer) throws IOException {
		BitTorrentHandshake handshake = peer.readHandshake();
		
		if (SHA1.match(peer.getTorrent().getHashArray(), handshake.getTorrentHash())) {
			return false;
		}
		
		peer.getClient().setReservedBytes(handshake.getPeerExtensionBytes());
		
		return true;
	}

	public int getFreeCapacity() {
		return maxPeers - peers.size();
	}

	public int getConnectingCount() {
		return peers.size();
	}

	public int getMaxCapacity() {
		return maxPeers;
	}

	@SuppressWarnings("unchecked")
	public int getConnectingCountFor(Torrent torrent) {
		LinkedList<Peer> peerList = null;
		
		synchronized (LOCK_PEER_LIST) {
			peerList = (LinkedList<Peer>) peers.clone();
		}
		
		return (int) peerList.stream().
				filter(p -> p.getTorrent().equals(torrent)).
				count();
	}

}
