package org.johnnei.javatorrent.internal.torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.network.PeerConnectionAccepter;
import org.johnnei.javatorrent.internal.network.PeerIoRunnable;
import org.johnnei.javatorrent.torrent.Torrent;

public class TorrentManager {

	private final Object TORRENTS_LOCK = new Object();

	private final TorrentClient torrentClient;

	private PeerConnectionAccepter connectorThread;
	private List<Torrent> activeTorrents;

	private LoopingRunnable peerIoRunnable;

	public TorrentManager(TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
		activeTorrents = new ArrayList<>();

		// Start reading peer input/output
		peerIoRunnable = new LoopingRunnable(new PeerIoRunnable(this));
	}

	/**
	 * Starts the connnection listener which will accept new peers
	 */
	public void start() {
		if (connectorThread != null && connectorThread.isAlive()) {
			return;
		}

		Thread thread = new Thread(peerIoRunnable, "Peer IO");
		thread.setDaemon(true);
		thread.start();

		try {
			connectorThread = new PeerConnectionAccepter(torrentClient);
			connectorThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Registers a new torrent
	 * @param torrent The torrent to register
	 */
	public void addTorrent(Torrent torrent) {
		synchronized (TORRENTS_LOCK) {
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
	 * Creates a shallow-copy of the list containing the torrents
	 * @return The list of torrents
	 */
	public Collection<Torrent> getTorrents() {
		synchronized (TORRENTS_LOCK) {
			return new ArrayList<>(activeTorrents);
		}
	}

}
