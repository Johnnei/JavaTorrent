package torrent.download.tracker;

import java.util.ArrayList;
import java.util.Random;

import org.johnnei.utils.ThreadUtils;

import torrent.download.Torrent;

public class TrackerManager extends Thread {
	
	private PeerConnectorPool peerConnectorPool;
	private ArrayList<Tracker> trackerList;
	private int transactionId;
	
	public TrackerManager() {
		super("Tracker Manager");
		trackerList = new ArrayList<>();
		transactionId = new Random().nextInt();
		peerConnectorPool = new PeerConnectorPool();
	}
	
	@Override
	public void run() {
		while(true) {
			for(int i = 0; i < trackerList.size(); i++) {
				Tracker tracker = trackerList.get(i);
				if(tracker.isValid()) {
					if(tracker.isConnected()) {
						ArrayList<TorrentInfo> torrentList = tracker.getTorrents();
						for(TorrentInfo torrentInfo : torrentList) {
							Torrent torrent = torrentInfo.getTorrent();
							//Check if torrent needs announce, else scrape
							if(torrent.needAnnounce()) {
								tracker.announce(torrent);
							}
						}
					} else {
						tracker.connect();
					}
				}
			}
			ThreadUtils.sleep(1000);
		}
	}
	
	/**
	 * Adds the torrent to the tracker and registers the tracker
	 * @param torrent The torrent to add
	 * @param tracker The tracker url
	 * @return The tracker on which the torrent has been added
	 */
	public Tracker addTorrent(Torrent torrent, String trackerUrl) {
		Tracker tracker = new Tracker(trackerUrl, peerConnectorPool);
		tracker = findTracker(tracker);
		tracker.addTorrent(torrent);
		return tracker;
	}
	
	/**
	 * Gets the tracker from the list or adds the given tracker
	 * @param tracker The tracker to find
	 * @return the tracker
	 */
	private Tracker findTracker(Tracker tracker) {
		for(Tracker t : trackerList) {
			if(t.equals(tracker)) {
				return t;
			}
		}
		trackerList.add(tracker);
		return tracker;
	}
	
	public int getTransactionId() {
		return transactionId++;
	}

	public int getConnectingCountFor(Torrent torrent) {
		return peerConnectorPool.getConnectingCountFor(torrent);
	}

}
