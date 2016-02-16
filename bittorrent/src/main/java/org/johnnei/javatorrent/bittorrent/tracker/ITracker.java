package org.johnnei.javatorrent.bittorrent.tracker;

import java.util.Optional;

import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.network.PeerConnectInfo;

public interface ITracker {

	/**
	 * Adds a torrent to the torrentMap if not already on it
	 * @param torrent The torrent to add
	 */
	void addTorrent(Torrent torrent);

	/**
	 * Checks if the tracker can request information about the torrent
	 * @param torrent The torrent we want information about
	 * @return if the tracker knows the torrent
	 */
	boolean hasTorrent(Torrent torrent);

	/**
	 * Requests the tracker to execute an announce event.
	 * In case the tracker cannot immediately execute the request the call may be ignored or postponed.
	 * This call must return immediately.
	 * @param torrent The torrent for which an announce event is being requested.
	 */
	void announce(Torrent torrent);

	/**
	 * Requests a scrape off all known torrent in this tracker.
	 * This call must return immediately.
	 */
	void scrape();

	/**
	 * Dispatches the connecting of a peer by the tracker. This call must return immediately.
	 * @param peer The peer connection information
	 */
	public void connectPeer(PeerConnectInfo peer);

	/**
	 * Retrieves the torrent information which is stored for this tracker.
	 * @param torrent The torrent which the information should be retrieved
	 * @return The torrent info or empty if not registered.
	 *
	 * @since 0.5
	 */
	Optional<TorrentInfo> getInfo(Torrent torrent);

	/**
	 * Returns the a user friendly name for the current action which the being executed.
	 * @return The name of the currently executing action
	 */
	String getStatus();

	/**
	 * Returns a user friendly name for this tracker.
	 * The recommended information to display is the domain of the tracker.
	 * @return The name of tracker
	 */
	String getName();

}
