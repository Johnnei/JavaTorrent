package torrent.download.tracker;

import java.io.IOException;
import java.util.LinkedList;

import org.johnnei.utils.ThreadUtils;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.download.peer.PeerConnectInfo;
import torrent.encoding.SHA1;
import torrent.network.BitTorrentSocket;
import torrent.protocol.BitTorrentHandshake;
import torrent.protocol.BitTorrentUtil;
import torrent.util.StringUtil;

public class PeerConnector implements Runnable {

	private final Object LOCK_PEER_LIST = new Object();
	
	/**
	 * The object on which the thread will start sleeping once it is out of work
	 */
	public final Object PEER_JOB_NOTIFY = new Object();
	
	/**
	 * List of peer that are currently being connected
	 */
	private LinkedList<PeerConnectInfo> peers;
	
	private TrackerManager manager;
	
	private final int maxPeers;
	
	public PeerConnector(TrackerManager manager, int maxConnecting) {
		this.manager = manager;
		this.maxPeers = maxConnecting;
		peers = new LinkedList<>();
	}

	/**
	 * Adds a pending connection peer to the connection cycle
	 * 
	 * @param peerInfo The peer to connect
	 */
	public void addPeer(PeerConnectInfo peerInfo) {
		if (peers.size() >= maxPeers) {
			throw new IllegalStateException(String.format("Connector is full"));
		}
		
		synchronized (LOCK_PEER_LIST) {
			peers.add(peerInfo);
		}
	}

	public void run() {
		while (true) {
			
			if (peers.isEmpty()) {
				ThreadUtils.wait(PEER_JOB_NOTIFY);
			}
			
			PeerConnectInfo peerInfo = null;
			BitTorrentSocket peerSocket = null;
			
			synchronized (LOCK_PEER_LIST) {
				peerInfo = peers.remove();
			}
			
			if (peerInfo == null) {
				continue;
			}
			
			try {
				peerSocket = new BitTorrentSocket();
				peerSocket.connect(peerInfo.getAddress());
				peerSocket.sendHandshake(manager.getPeerId(), peerInfo.getTorrent().getHashArray());
				
				long timeWaited = 0;
				while (!peerSocket.canReadMessage() && timeWaited < 10_000) {
					final int INTERVAL = 100;
					ThreadUtils.sleep(INTERVAL);
					timeWaited += INTERVAL;
				}
				
				if (!peerSocket.canReadMessage()) {
					throw new IOException("Handshake timeout");
				}
				
				BitTorrentHandshake handshake = checkHandshake(peerSocket, peerInfo.getTorrent().getHashArray());
				
				Peer peer = new Peer(peerSocket, peerInfo.getTorrent());
				peer.getExtensions().register(handshake.getPeerExtensionBytes());
				BitTorrentUtil.onPostHandshake(peer);
			} catch (IOException e) {
				System.err.println(String.format("[PeerConnector] Failed to connect to peer (%s:%d): %s", peerInfo.getAddress().getAddress(), peerInfo.getAddress().getPort(), e.getMessage()));
				if (peerSocket != null) {
					peerSocket.close();
				}
			}
		}
	}
	
	private BitTorrentHandshake checkHandshake(BitTorrentSocket peerSocket, byte[] torrentHash) throws IOException {
		BitTorrentHandshake handshake = peerSocket.readHandshake();
		
		if (!SHA1.match(torrentHash, handshake.getTorrentHash())) {
			throw new IOException(String.format("Peer does not download the same torrent (Expected: %s, Got: %s)", StringUtil.byteArrayToString(torrentHash), StringUtil.byteArrayToString(handshake.getTorrentHash())));
		}
		
		return handshake;
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

	public int getConnectingCountFor(Torrent torrent) {
		LinkedList<PeerConnectInfo> peerList = null;
		
		synchronized (LOCK_PEER_LIST) {
			peerList = new LinkedList<>(peers);
		}
		
		return (int) peerList.stream().
				filter(p -> p.getTorrent().equals(torrent)).
				count();
	}

}
