package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.PeerConnectionAccepter;
import org.johnnei.javatorrent.network.socket.PeersReadRunnable;
import org.johnnei.javatorrent.network.socket.PeersWriterRunnable;
import org.johnnei.javatorrent.tracker.TrackerManager;

public class TorrentManager {

	private final Object TORRENTS_LOCK = new Object();

	private final TorrentClient torrentClient;

	private PeerConnectionAccepter connectorThread;
	private ArrayList<Torrent> activeTorrents;

	private PeersReadRunnable peerReader;
	private PeersWriterRunnable peerWriter;
	private Thread[] peerThreads;

	public TorrentManager(TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
		activeTorrents = new ArrayList<>();

		// Start reading peer input/output
		peerReader = new PeersReadRunnable(this);
		peerWriter = new PeersWriterRunnable(this);

		peerThreads = new Thread[2];
		peerThreads[0] = new Thread(peerReader, "Peer input reader");
		peerThreads[1] = new Thread(peerWriter, "Peer output writer");

		for(Thread thread : peerThreads) {
			thread.setDaemon(true);
			thread.start();
		}
	}

	/**
	 * Starts the connnection listener which will accept new peers
	 * @param trackerManager the tracker manager which will assign the peers
	 */
	public void startListener(TrackerManager trackerManager) {
		if (connectorThread != null && connectorThread.isAlive()) {
			return;
		}

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
