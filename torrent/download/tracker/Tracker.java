package torrent.download.tracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import torrent.download.Torrent;

public class Tracker {
	
	public static final int SCRAPE_INTERVAL = 10000;
	public static final int DEFAULT_ANNOUNCE_INTERVAL = 30000;
	public static final int CONNECTION_DURATION = 300000; //5 minutes
	
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
	
	public Tracker(String url, PeerConnectorPool peerConnectorPool, TrackerManager manager) {
		connection = new TrackerConnection(url, peerConnectorPool, manager);
		torrentMap = new HashMap<>();
		announceInterval = DEFAULT_ANNOUNCE_INTERVAL;
		lastScrapeTime = System.currentTimeMillis() - SCRAPE_INTERVAL;
		connectionTime = System.currentTimeMillis() - CONNECTION_DURATION;
	}
	
	/**
	 * Adds a torrent to the torrentMap if not already on it
	 * @param torrent The torrent to add
	 */
	public void addTorrent(Torrent torrent) {
		if(!torrentMap.containsKey(torrent.getHash())) {
			synchronized (this) {
				torrentMap.put(torrent.getHash(), new TorrentInfo(torrent));
			}
		}
	}
	
	/**
	 * Checks if the tracker can request information about the torrent
	 * @param torrent The torrent we want information about
	 * @return if the tracker knows the torrent
	 */
	public boolean hasTorrent(Torrent torrent) {
		return torrentMap.containsKey(torrent.getHash());
	}
	
	/**
	 * Finds the torrent info associated to the given torrent
	 * @param torrent The torrent on which we want info on
	 * @return The info or null if not found
	 */
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
		connection.log(e.getMessage(), true);
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
		return errorCount < 3;
	}
	
	/**
	 * Checks if the tracker is not on timeout for the given torrent
	 * @param torrent the torrent which needs announcing
	 * @return
	 */
	public boolean canAnnounce(Torrent torrent) {
		TorrentInfo torrentInfo = torrentMap.get(torrent.getHash());
		return torrentInfo.getTimeSinceLastAnnouce() >= announceInterval;
	}
	
	public boolean isConnected() {
		return (System.currentTimeMillis() - connectionTime) <= CONNECTION_DURATION && connection.isConnected();
	}
	
	public ArrayList<TorrentInfo> getTorrents() {
		ArrayList<TorrentInfo> torrents = new ArrayList<>(torrentMap.size());
		
		synchronized (this) {
			Iterator<Entry<String, TorrentInfo>> iterator = torrentMap.entrySet().iterator();
			while(iterator.hasNext()) {
				Entry<String, TorrentInfo> entry = iterator.next();
				torrents.add(entry.getValue());
			}
		}
		
		return torrents;
	}
	
	public String getName() {
		return connection.getTrackerName();
	}
	
	public String getStatus() {
		return connection.getStatus();
	}

	public void connect() {
		try {
			connection.connect();
			connectionTime = System.currentTimeMillis();
			onSucces();
		} catch (TrackerException e) {
			onError(e);
		}
	}

}
