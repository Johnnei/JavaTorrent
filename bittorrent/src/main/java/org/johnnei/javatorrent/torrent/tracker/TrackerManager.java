package org.johnnei.javatorrent.torrent.tracker;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.johnnei.javatorrent.Version;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

/**
 * Managers the trackers which are used to collect {@link Peer}s for {@link Torrent}s
 * @author Johnnei
 *
 */
public class TrackerManager {

	private final TrackerFactory trackerFactory;

	private byte[] peerId;

	private IPeerConnector peerConnector;

	private AtomicInteger transactionId;

	public TrackerManager(IPeerConnector peerConnector, TrackerFactory trackerFactory) {
		this.trackerFactory = trackerFactory;
		this.peerConnector = peerConnector;

		Random random = new Random();
		transactionId = new AtomicInteger(random.nextInt());

		char[] version = Version.BUILD.split(" ")[1].replace(".", "").toCharArray();
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
			peerId[i] = (byte) (random.nextInt() & 0xFF);
		}
	}

	/**
	 * Invokes the {@link ITracker#announce(Torrent)} method for all trackers having the given torrent.
	 * @param torrent The torrent to announce.
	 */
	public void announce(Torrent torrent) {
		getTrackersFor(torrent).forEach(tracker -> tracker.announce(torrent));
	}

	/**
	 * Adds the torrent to the tracker and registers the tracker.
	 * If the tracker cannot be resolved this call will have no side-effects.
	 * @param torrent The torrent to add
	 * @param tracker The tracker url
	 */
	public void addTorrent(Torrent torrent, String trackerUrl) {
		final Optional<ITracker> tracker = getTrackerFor(trackerUrl);
		if (!tracker.isPresent()) {
			return;
		}

		tracker.get().addTorrent(torrent);
	}

	/**
	 * Gets the tracker from the list or adds the given tracker
	 * @param tracker The tracker to find
	 * @return the tracker
	 */
	private Optional<ITracker> getTrackerFor(String trackerUrl) {
		return trackerFactory.getTrackerFor(trackerUrl);
	}

	public int createUniqueTransactionId() {
		return transactionId.incrementAndGet();
	}

	public int getConnectingCountFor(Torrent torrent) {
		return peerConnector.getConnectingCountFor(torrent);
	}

	/**
	 * Gets all trackers which know the given torrent
	 * @param torrent the torrent which the tracker must support
	 * @return a collection of trackers which support the given torrent
	 */
	public List<ITracker> getTrackersFor(Torrent torrent) {
		return trackerFactory.getTrackingsHavingTorrent(torrent);
	}

	/**
	 * Gets the peer ID associated to this tracker manager
	 *
	 * @return
	 */
	public byte[] getPeerId() {
		return peerId;
	}

}
