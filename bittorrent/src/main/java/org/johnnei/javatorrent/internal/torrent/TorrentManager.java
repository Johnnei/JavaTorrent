package org.johnnei.javatorrent.internal.torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.internal.network.PeerIoRunnable;
import org.johnnei.javatorrent.network.PeerConnectionAcceptor;
import org.johnnei.javatorrent.torrent.Torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(TorrentManager.class);

	private final Object torrentListLock = new Object();

	private TorrentClient torrentClient;

	private List<TorrentPair> activeTorrents;

	private LoopingRunnable connectorRunnable;

	private LoopingRunnable peerIoRunnable;

	public TorrentManager() {
		activeTorrents = new ArrayList<>();
	}

	/**
	 * Starts the connnection listener which will accept new peers
	 */
	public void start(TorrentClient torrentClient) {
		this.torrentClient = torrentClient;

		// Start reading peer input/output
		peerIoRunnable = new LoopingRunnable(new PeerIoRunnable(this));
		Thread thread = new Thread(peerIoRunnable, "Peer IO");
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Attempts to start a server socket to accept incoming TCP connections.
	 */
	public void enableConnectionAcceptor() {
		try {
			connectorRunnable = new LoopingRunnable(new PeerConnectionAcceptor(torrentClient));
			Thread connectorThread = new Thread(connectorRunnable, "Connection Acceptor");
			connectorThread.setDaemon(true);
			connectorThread.start();
		} catch (IOException e) {
			LOGGER.warn("Failed to start connection acceptor", e);
		}
	}

	/**
	 * Gracefully stops the connection processing.
	 */
	public void stop() {
		peerIoRunnable.stop();

		if (connectorRunnable != null) {
			connectorRunnable.stop();
		}
	}

	/**
	 * Registers a new torrent
	 * @param torrent The torrent to register
	 */
	public void addTorrent(Torrent torrent) {
		synchronized (torrentListLock) {
			activeTorrents.add(new TorrentPair(this, torrentClient, torrent));
		}
	}

	public void removeTorrent(Torrent torrent) {
		synchronized (torrentListLock) {
			activeTorrents.remove(torrent);
		}
	}

	/**
	 * Gets the torrent associated with the given hash.
	 * @param hash The BTIH of the torrent
	 * @return The torrent if known.
	 */
	public Optional<Torrent> getTorrent(byte[] hash) {
		return activeTorrents.stream()
				.map(TorrentPair::getTorrent)
				.filter(torrent -> Arrays.equals(torrent.getHashArray(), hash))
				.findAny();
	}

	/**
	 * Creates a copy of the list containing the torrents
	 * @return The list of torrents
	 */
	public Collection<Torrent> getTorrents() {
		synchronized (torrentListLock) {
			return activeTorrents.stream().map(TorrentPair::getTorrent).collect(Collectors.toList());
		}
	}

	private final class TorrentPair {

		private final Torrent torrent;

		private final TorrentProcessor torrentProcessor;

		TorrentPair(TorrentManager torrentManager, TorrentClient torrentClient, Torrent torrent) {
			this.torrent = torrent;
			torrentProcessor = new TorrentProcessor(torrentManager, torrentClient, torrent);
		}

		public Torrent getTorrent() {
			return torrent;
		}

		public TorrentProcessor getTorrentProcessor() {
			return torrentProcessor;
		}
	}

}
