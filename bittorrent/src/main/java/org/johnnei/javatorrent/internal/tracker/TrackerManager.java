package org.johnnei.javatorrent.internal.tracker;

import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerFactory;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.tracker.IPeerConnector;

/**
 * Managers the trackers which are used to collect {@link Peer}s for {@link Torrent}s
 * @author Johnnei
 *
 */
public class TrackerManager {

	private final TrackerFactory trackerFactory;

	private IPeerConnector peerConnector;

	public TrackerManager(IPeerConnector peerConnector, TrackerFactory trackerFactory) {
		this.trackerFactory = trackerFactory;
		this.peerConnector = peerConnector;

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
	 * @param trackerUrl The tracker url
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
	 * @param trackerUrl The tracker to find
	 * @return the tracker
	 */
	private Optional<ITracker> getTrackerFor(String trackerUrl) {
		return trackerFactory.getTrackerFor(trackerUrl);
	}

	/**
	 * Calculates how many connections are assigned to the torrent but haven't passed the BitTorrent handshake yet.
	 * @param torrent The torrent for which connections must be counted.
	 * @return The amount of pending peers.
	 */
	public int getConnectingCountFor(Torrent torrent) {
		return peerConnector.getConnectingCountFor(torrent);
	}

	/**
	 * Gets all trackers which know the given torrent
	 * @param torrent the torrent which the tracker must support
	 * @return a collection of trackers which support the given torrent
	 */
	public List<ITracker> getTrackersFor(Torrent torrent) {
		return trackerFactory.getTrackersHavingTorrent(torrent);
	}

}
