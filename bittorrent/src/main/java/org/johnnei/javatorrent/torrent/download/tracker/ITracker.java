package org.johnnei.javatorrent.torrent.download.tracker;

import java.util.ArrayList;

import org.johnnei.javatorrent.torrent.download.Torrent;

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
	 * @param torrent The torrent for which an announce event is being requested.
	 */
	void announce(Torrent torrent);

	/**
	 * Checks if the tracker is not on timeout for the given torrent
	 * @param torrent the torrent which needs announcing
	 * @return
	 *
	 * @deprecated The announce calls are no longer mandatory to be honored.
	 */
	@Deprecated
	boolean canAnnounce(Torrent torrent);

	@Deprecated
	ArrayList<TorrentInfo> getTorrents();

	void scrape();

	TorrentInfo getInfo(Torrent torrent);

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
