package torrent.download;

import java.util.Collection;
import java.util.LinkedList;

import torrent.TorrentManager;
import torrent.download.tracker.Tracker;
import torrent.download.tracker.TrackerManager;

public class TorrentBuilder {
	
	private byte[] btihHash;
	private String displayName;
	
	/**
	 * The collection of tracker urls
	 */
	private Collection<String> trackers;
	
	public TorrentBuilder() {
		trackers = new LinkedList<>();
	}
	
	public void setHash(byte[] btihHash) {
		this.btihHash = btihHash;
	}
	
	public void addTracker(String trackerUrl) {
		trackers.add(trackerUrl);
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	/**
	 * Creates the {@link Torrent} derived from the given parameters in previous calls
	 * @param torrentManager
	 * @param trackerManager
	 * @return
	 * @throws IllegalStateException if the torrent has to few information to start downloading
	 */
	public Torrent build(TorrentManager torrentManager, TrackerManager trackerManager) throws IllegalStateException {
		if (btihHash == null) {
			throw new IllegalStateException("Missing SHA-1 hash for torrent");
		}
		
		if (trackers.isEmpty()) {
			throw new IllegalStateException("Missing trackers for torrent");
		}
		
		Torrent torrent = new Torrent(torrentManager, btihHash, displayName);
		
		trackers.forEach(trackerUrl -> {
			Tracker tracker = trackerManager.addTorrent(torrent, trackerUrl);
			torrent.addTracker(tracker);
		});
		
		return torrent;
	}

}
