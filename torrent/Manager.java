package torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

import torrent.download.PeersReadRunnable;
import torrent.download.PeersWriterRunnable;
import torrent.download.Torrent;
import torrent.download.tracker.TrackerManager;

public class Manager {

	private static Manager manager = new Manager();

	public static Manager getManager() {
		return manager;
	}
	
	public static TrackerManager getTrackerManager() {
		return manager.trackerManager;
	}
	
	public final Object TORRENTS_LOCK = new Object();

	private PeerConnectionAccepter connectorThread;
	private TrackerManager trackerManager;
	private byte[] peerId;
	private ArrayList<Torrent> activeTorrents;
	
	private PeersReadRunnable peerReader;
	private PeersWriterRunnable peerWriter;
	private Thread[] peerThreads;

	private Manager() {
		activeTorrents = new ArrayList<>();
		try {
			connectorThread = new PeerConnectionAccepter();
			connectorThread.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		char[] version = JavaTorrent.BUILD.split(" ")[1].replace(".", "").toCharArray();
		peerId = new byte[20];
		peerId[0] = '-';
		peerId[1] = 'J';
		peerId[2] = 'T';
		peerId[3] = (byte) version[0];
		peerId[4] = (byte) version[1];
		peerId[5] = (byte) version[2];
		peerId[6] = (byte) version[3];
		peerId[7] = '-';
		for (int i = 8; i < peerId.length; i++) {
			peerId[i] = (byte) (new Random().nextInt() & 0xFF);
		}
		
		// Start tracker management
		trackerManager = new TrackerManager();
		trackerManager.start();
		
		// Start reading peer input/output
		peerReader = new PeersReadRunnable(this);
		peerWriter = new PeersWriterRunnable(this);
		
		peerThreads = new Thread[2];
		peerThreads[0] = new Thread(peerReader, "Peer input reader");
		peerThreads[1] = new Thread(peerWriter, "Peer output writer");
		
		for(Thread thread : peerThreads) {
			thread.start();
		}
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

	public byte[] getPeer() {
		return peerId;
	}

	public static byte[] getPeerId() {
		return getManager().getPeer();
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

}
