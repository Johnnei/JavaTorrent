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

	void announce(Torrent torrent);

	/**
	 * Checks if the tracker is not on timeout for the given torrent
	 * @param torrent the torrent which needs announcing
	 * @return
	 */
	boolean canAnnounce(Torrent torrent);

	ArrayList<TorrentInfo> getTorrents();

	void scrape();

	TorrentInfo getInfo(Torrent torrent);

	String getStatus();

	String getName();

}
