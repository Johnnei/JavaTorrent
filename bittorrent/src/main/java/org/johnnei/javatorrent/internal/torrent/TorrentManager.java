package org.johnnei.javatorrent.internal.torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.network.PeerConnectionAcceptor;
import org.johnnei.javatorrent.internal.network.PeerIoRunnable;
import org.johnnei.javatorrent.torrent.Torrent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TorrentManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(TorrentManager.class);

	private final Object torrentListLock = new Object();

	private List<Torrent> activeTorrents;

	private LoopingRunnable connectorRunnable;

	private LoopingRunnable peerIoRunnable;

	public TorrentManager() {
		activeTorrents = new ArrayList<>();
	}

	/**
	 * Starts the connnection listener which will accept new peers
	 */
	public void start(TorrentClient torrentClient) {
		// Start reading peer input/output
		peerIoRunnable = new LoopingRunnable(new PeerIoRunnable(this));
		Thread thread = new Thread(peerIoRunnable, "Peer IO");
		thread.setDaemon(true);
		thread.start();

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
		connectorRunnable.stop();
	}

	/**
	 * Registers a new torrent
	 * @param torrent The torrent to register
	 */
	public void addTorrent(Torrent torrent) {
		synchronized (torrentListLock) {
			activeTorrents.add(torrent);
		}
	}

	/**
	 * Gets the torrent associated with the given hash.
	 * @param hash The BTIH of the torrent
	 * @return The torrent if known.
	 */
	public Optional<Torrent> getTorrent(byte[] hash) {
		for (Torrent torrent : activeTorrents) {
			if (Arrays.equals(torrent.getHashArray(), hash)) {
				return Optional.of(torrent);
			}
		}

		return Optional.empty();
	}

	/**
	 * Creates an unmodifiable copy of the list containing the torrents
	 * @return The list of torrents
	 */
	public Collection<Torrent> getTorrents() {
		synchronized (torrentListLock) {
			return Collections.unmodifiableCollection(activeTorrents);
		}
	}

}
