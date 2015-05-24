package torrent.download.tracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.johnnei.utils.ThreadUtils;

import torrent.JavaTorrent;
import torrent.download.Torrent;
import torrent.download.peer.Peer;

/**
 * Managers the trackers which are used to collect {@link Peer}s for {@link Torrent}s
 * @author Johnnei
 *
 */
public class TrackerManager implements Runnable {
	
	private byte[] peerId;
	private PeerConnectorPool peerConnectorPool;
	private ArrayList<Tracker> trackerList;
	private int transactionId;
	
	public TrackerManager() {
		trackerList = new ArrayList<>();
		transactionId = new Random().nextInt();
		peerConnectorPool = new PeerConnectorPool(this);
		
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
	}
	
	@Override
	public void run() {
		while(true) {
			for(int i = 0; i < trackerList.size(); i++) {
				Tracker tracker = trackerList.get(i);
				
				if (!tracker.isValid()) {
					continue;
				}
				
				if (tracker.isConnected()) {
					List<TorrentInfo> torrentList = tracker.getTorrents();
					for(TorrentInfo torrentInfo : torrentList) {
						Torrent torrent = torrentInfo.getTorrent();
						
						if (!tracker.canAnnounce(torrent)) {
							// Tracker is still on timeout
							continue;
						}
						
						//Check if torrent needs announce, else scrape
						if(torrent.needAnnounce()) {
							tracker.announce(torrent);
						}
					}
				} else {
					tracker.connect();
				}
				
				if(tracker.isValid()) {
					if(tracker.isConnected()) {
						
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
	 */
	public void addTorrent(Torrent torrent, String trackerUrl) {
		Tracker tracker = new Tracker(trackerUrl, peerConnectorPool, this);
		
		if (!tracker.isValid()) {
			return;
		}
		
		tracker = findTracker(tracker);
		tracker.addTorrent(torrent);
	}
	
	/**
	 * Gets the tracker from the list or adds the given tracker
	 * @param tracker The tracker to find
	 * @return the tracker
	 */
	private Tracker findTracker(Tracker tracker) {
		Optional<Tracker> optionalTracker = trackerList.stream().filter(t -> t.getInetAddress().equals(tracker.getInetAddress())).findAny();
		
		if (optionalTracker.isPresent()) {
			return optionalTracker.get();
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
	
	/**
	 * Gets all trackers which know the given torrent
	 * @param torrent the torrent which the tracker must support
	 * @return a collection of trackers which support the given torrent
	 */
	public List<Tracker> getTrackersFor(Torrent torrent) {
		return trackerList.stream().filter(tracker -> tracker.hasTorrent(torrent)).collect(Collectors.toList());
	}
	
	/**
	 * Gets the peer ID associated to this tracker manager
	 * @return
	 */
	public byte[] getPeerId() {
		return peerId;
	}

}
