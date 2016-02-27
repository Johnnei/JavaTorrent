package org.johnnei.javatorrent.tracker;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentUtil;
import org.johnnei.javatorrent.utils.StringUtils;
import org.johnnei.javatorrent.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerConnector implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnector.class);

	private final Object LOCK_PEER_LIST = new Object();

	/**
	 * The object on which the thread will start sleeping once it is out of work
	 */
	public final Object PEER_JOB_NOTIFY = new Object();


	/**
	 * List of peer that are currently being connected
	 */
	private LinkedList<PeerConnectInfo> peers;

	private final int maxPeers;

	private final TorrentClient torrentClient;

	public PeerConnector(TorrentClient torrentClient, int maxConnecting) {
		this.maxPeers = maxConnecting;
		this.torrentClient = torrentClient;
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

	@Override
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

			peerSocket = new BitTorrentSocket(torrentClient.getMessageFactory());
			try {
				peerSocket.connect(torrentClient.getConnectionDegradation(), peerInfo.getAddress());
				peerSocket.sendHandshake(
						torrentClient.getExtensionBytes(),
						torrentClient.getPeerId(),
						peerInfo.getTorrent().getHashArray());

				long timeWaited = 0;
				while (!peerSocket.canReadMessage() && timeWaited < 10_000) {
					final int INTERVAL = 100;
					ThreadUtils.sleep(INTERVAL);
					timeWaited += INTERVAL;
				}

				if (!peerSocket.canReadMessage()) {
					throw new IOException(String.format("Handshake timeout (%s)", peerSocket.getHandshakeProgress()));
				}

				BitTorrentHandshake handshake = checkHandshake(peerSocket, peerInfo.getTorrent().getHashArray());

				Peer peer = new Peer(peerSocket, peerInfo.getTorrent(), handshake.getPeerExtensionBytes());
				BitTorrentUtil.onPostHandshake(peer);
				LOGGER.debug("Connected with {}: {}", peerInfo.getAddress().getAddress(), peerInfo.getAddress().getPort());
			} catch (IOException e) {
				LOGGER.debug("Failed to connect to peer ({}:{})", peerInfo.getAddress().getAddress(), peerInfo.getAddress().getPort(), e);
				peerSocket.close();
			}
		}
	}

	private BitTorrentHandshake checkHandshake(BitTorrentSocket peerSocket, byte[] torrentHash) throws IOException {
		BitTorrentHandshake handshake = peerSocket.readHandshake();

		if (!Arrays.equals(torrentHash, handshake.getTorrentHash())) {
			throw new IOException(String.format("Peer does not download the same torrent (Expected: %s, Got: %s)", StringUtils.byteArrayToString(torrentHash), StringUtils.byteArrayToString(handshake.getTorrentHash())));
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
