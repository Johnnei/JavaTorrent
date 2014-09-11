package torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import torrent.download.PeersReadRunnable;
import torrent.download.PeersWriterRunnable;
import torrent.download.Torrent;
import torrent.download.tracker.TrackerManager;

public class TorrentManager {

	private final Object TORRENTS_LOCK = new Object();

	private PeerConnectionAccepter connectorThread;
	private TrackerManager trackerManager;
	private Thread trackerManagerThread;
	private ArrayList<Torrent> activeTorrents;
	
	private PeersReadRunnable peerReader;
	private PeersWriterRunnable peerWriter;
	private Thread[] peerThreads;

	public TorrentManager() {
		activeTorrents = new ArrayList<>();
		try {
			connectorThread = new PeerConnectionAccepter(this);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Start tracker management
		trackerManager = new TrackerManager(this);
		trackerManagerThread = new Thread(trackerManager, "Tracker manager");
		trackerManagerThread.start();
		
		// Start reading peer input/output
		peerReader = new PeersReadRunnable(this);
		peerWriter = new PeersWriterRunnable(this);
		
		peerThreads = new Thread[2];
		peerThreads[0] = new Thread(peerReader, "Peer input reader");
		peerThreads[1] = new Thread(peerWriter, "Peer output writer");
		
		for(Thread thread : peerThreads) {
			thread.start();
		}
		
		connectorThread.start();
	}

	public void addTorrent(Torrent torrent) {
		synchronized (TORRENTS_LOCK) {
			activeTorrents.add(torrent);
		}
	}

	public Torrent getTorrent(String hash) {
		for (int i = 0; i < activeTorrents.size(); i++) {
			Torrent t = activeTorrents.get(i);
			if (t.getHash().equals(hash))
				return t;
		}
		return null;
	}
	
	/**
	 * Creates a shallow-copy of the list containing the torrents
	 * @return
	 */
	public Collection<Torrent> getTorrents() {
		synchronized (TORRENTS_LOCK) {
			return new ArrayList<Torrent>(activeTorrents);
		}
	}
	
	/**
	 * Gets the tracker manager which manages the trackers
	 * @return
	 */
	public TrackerManager getTrackerManager() {
		return trackerManager;
	}

}
