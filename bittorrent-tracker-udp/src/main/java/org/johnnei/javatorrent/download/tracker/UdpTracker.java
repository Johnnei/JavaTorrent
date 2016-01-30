package org.johnnei.javatorrent.download.tracker;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.tracker.IPeerConnector;
import org.johnnei.javatorrent.torrent.download.tracker.ITracker;
import org.johnnei.javatorrent.torrent.download.tracker.TorrentInfo;
import org.johnnei.javatorrent.torrent.download.tracker.TrackerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UdpTracker implements ITracker {

	public static final int SCRAPE_INTERVAL = 10000;
	public static final int DEFAULT_ANNOUNCE_INTERVAL = 30000;
	public static final int CONNECTION_DURATION = 300000; //5 minutes

	private static final Logger LOGGER = LoggerFactory.getLogger(UdpTracker.class);

	private TrackerConnection connection;
	/**
	 * A hashmap containing all torrents which this tracker is supposed to know
	 */
	private HashMap<String, TorrentInfo> torrentMap;
	/**
	 * The timestamp of the last scrape
	 */
	private long lastScrapeTime;
	/**
	 * The minimum announce interval as requested by the tracker
	 */
	private int announceInterval;
	/**
	 * The time when the last connection id has been requested
	 */
	private long connectionTime;
	/**
	 * The amount of tracker errors detected<br/>
	 * Successful operation decrease errorCount by 1 to a minimum of 0
	 */
	private int errorCount;

	public UdpTracker(String url, IPeerConnector peerConnector) {
		connection = new TrackerConnection(url, peerConnector);
		torrentMap = new HashMap<>();
		announceInterval = DEFAULT_ANNOUNCE_INTERVAL;
		lastScrapeTime = System.currentTimeMillis() - SCRAPE_INTERVAL;
		connectionTime = System.currentTimeMillis() - CONNECTION_DURATION;
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.ITracker#addTorrent(torrent.download.Torrent)
	 */
	@Override
	public void addTorrent(Torrent torrent) {
		if(!torrentMap.containsKey(torrent.getHash())) {
			synchronized (this) {
				torrentMap.put(torrent.getHash(), new TorrentInfo(torrent));
			}
		}
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.ITracker#hasTorrent(torrent.download.Torrent)
	 */
	@Override
	public boolean hasTorrent(Torrent torrent) {
		return torrentMap.containsKey(torrent.getHash());
	}

	/**
	 * Finds the torrent info associated to the given torrent
	 * @param torrent The torrent on which we want info on
	 * @return The info or null if not found
	 */
	@Override
	public TorrentInfo getInfo(Torrent torrent) {
		synchronized (this) {
			Iterator<Entry<String, TorrentInfo>> iterator = torrentMap.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<String, TorrentInfo> entry = iterator.next();
				TorrentInfo torrentInfo = entry.getValue();
				if(torrentInfo.getTorrent().equals(torrent)) {
					return torrentInfo;
				}
			}
		}
		return null;
	}

	/**
	 * Scrapes all torrents for this tracker
	 * TODO Improve the tracker implementation to allow multiple torrents to be scraped at once
	 */
	@Override
	public void scrape() {
		if(System.currentTimeMillis() - lastScrapeTime >= SCRAPE_INTERVAL) {
			synchronized (this) {
				Iterator<Entry<String, TorrentInfo>> iterator = torrentMap.entrySet().iterator();
				while(iterator.hasNext()) {
					Entry<String, TorrentInfo> entry = iterator.next();
					TorrentInfo torrentInfo = entry.getValue();
					try {
						connection.scrape(torrentInfo);
						onSucces();
					} catch (TrackerException e) {
						onError(e);
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.ITracker#announce(torrent.download.Torrent)
	 */
	@Override
	public void announce(Torrent torrent) {
		TorrentInfo torrentInfo = torrentMap.get(torrent.getHash());
		try {
			connection.announce(torrentInfo);
			torrentInfo.updateAnnounceTime();
			onSucces();
		} catch (TrackerException e) {
			onError(e);
		}
	}

	public void onError(Exception e) {
		LOGGER.warn(e.getMessage(), e);
		errorCount++;
	}

	public void onSucces() {
		errorCount = Math.max(errorCount - 1, 0);
	}

	/**
	 * Checks if this tracker still validates to be checked<br/>
	 * @return true if less than 3 errors occured
	 */
	public boolean isValid() {
		return errorCount < 3 && connection.getAddress() != null;
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.ITracker#canAnnounce(torrent.download.Torrent)
	 */
	@Override
	public boolean canAnnounce(Torrent torrent) {
		TorrentInfo torrentInfo = torrentMap.get(torrent.getHash());
		return torrentInfo.getTimeSinceLastAnnouce() >= announceInterval;
	}

	public boolean isConnected() {
		return (System.currentTimeMillis() - connectionTime) <= CONNECTION_DURATION && connection.isConnected();
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.ITracker#getTorrents()
	 */
	@Override
	public ArrayList<TorrentInfo> getTorrents() {
		ArrayList<TorrentInfo> torrents = new ArrayList<>(torrentMap.size());

		synchronized (this) {
			for (Entry<String, TorrentInfo> entry : torrentMap.entrySet()) {
				torrents.add(entry.getValue());
			}
		}

		return torrents;
	}

	@Override
	public String getName() {
		return connection.getTrackerName();
	}

	@Override
	public String getStatus() {
		return connection.getStatus();
	}

	public InetAddress getInetAddress() {
		return connection.getAddress();
	}

	public void connect() {
		try {
			connection.connect();
			connectionTime = System.currentTimeMillis();
		} catch (TrackerException e) {
			onError(e);
		}
	}

}
