package org.johnnei.javatorrent.tracker;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.utils.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerConnector implements Runnable, IPeerConnector {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnector.class);

	private final Object peerListLock = new Object();

	/**
	 * The object on which the thread will start sleeping once it is out of work
	 */
	private final Lock newPeerLock = new ReentrantLock();

	private final Condition newPeerCondition = newPeerLock.newCondition();

	/**
	 * List of peer that are currently being connected
	 */
	private LinkedList<PeerConnectInfo> peers;

	private final LoopingRunnable runnable;

	private final TorrentClient torrentClient;

	public PeerConnector(TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
		runnable = new LoopingRunnable(this);
		peers = new LinkedList<>();
	}

	/**
	 * Adds a pending connection peer to the connection cycle
	 *
	 * @param peerInfo The peer to connect
	 */
	@Override
	public void enqueuePeer(PeerConnectInfo peerInfo) {
		if (peerInfo == null) {
			// Bad user!
			return;
		}

		synchronized (peerListLock) {
			peers.add(peerInfo);
		}
		newPeerLock.lock();
		try {
			newPeerCondition.signal();
		} finally {
			newPeerLock.unlock();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void start() {
		Thread thread = new Thread(runnable, "Peer Connector");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() {
		runnable.stop();
	}

	/**
	 * Attempts to establish and process the handshake of a single peer.
	 *
	 * @see LoopingRunnable
	 */
	@Override
	public void run() {
		while (peers.isEmpty()) {
			newPeerLock.lock();
			try {
				newPeerCondition.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			} finally {
				newPeerLock.unlock();
			}
		}

		PeerConnectInfo peerInfo;

		synchronized (peerListLock) {
			peerInfo = peers.remove();
		}

		BitTorrentSocket peerSocket = createUnconnectedSocket();
		try {
			peerSocket.connect(torrentClient.getConnectionDegradation(), peerInfo.getAddress());
			peerSocket.sendHandshake(torrentClient.getExtensionBytes(), torrentClient.getPeerId(), peerInfo.getTorrent().getHashArray());
			BitTorrentHandshake handshake = checkHandshake(peerSocket, peerInfo.getTorrent().getHashArray());
			Peer peer = new Peer(peerSocket, peerInfo.getTorrent(), handshake.getPeerExtensionBytes());
			peerInfo.getTorrent().addPeer(peer);
			LOGGER.debug("Connected with {}: {}", peerInfo.getAddress().getAddress(), peerInfo.getAddress().getPort());
		} catch (IOException e) {
			LOGGER.debug("Failed to connect to peer ({}:{})", peerInfo.getAddress().getAddress(), peerInfo.getAddress().getPort(), e);
			peerSocket.close();
		}
	}

	BitTorrentSocket createUnconnectedSocket() {
		return new BitTorrentSocket(torrentClient.getMessageFactory());
	}

	private BitTorrentHandshake checkHandshake(BitTorrentSocket peerSocket, byte[] torrentHash) throws IOException {
		BitTorrentHandshake handshake = peerSocket.readHandshake();

		if (!Arrays.equals(torrentHash, handshake.getTorrentHash())) {
			throw new IOException(String.format("Peer does not download the same torrent (Expected: %s, Got: %s)", StringUtils.byteArrayToString(torrentHash), StringUtils.byteArrayToString(handshake.getTorrentHash())));
		}

		return handshake;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getConnectingCount() {
		return peers.size();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getConnectingCountFor(Torrent torrent) {
		LinkedList<PeerConnectInfo> peerList;

		synchronized (peerListLock) {
			peerList = new LinkedList<>(peers);
		}

		return (int) peerList.stream().
				filter(p -> p.getTorrent().equals(torrent)).
				count();
	}

}
