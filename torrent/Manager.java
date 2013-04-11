package torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

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

	private PeerConnector connectorThread;
	private TrackerManager trackerManager;
	private byte[] peerId;
	private ArrayList<Torrent> activeTorrents;

	private Manager() {
		activeTorrents = new ArrayList<>();
		try {
			connectorThread = new PeerConnector();
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
		trackerManager = new TrackerManager();
		trackerManager.start();
	}

	public void addTorrent(Torrent torrent) {
		activeTorrents.add(torrent);
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

}
