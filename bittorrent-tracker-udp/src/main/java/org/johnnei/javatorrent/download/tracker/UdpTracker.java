package org.johnnei.javatorrent.download.tracker;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.async.CallbackFuture;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.tracker.ITracker;
import org.johnnei.javatorrent.torrent.download.tracker.TorrentInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the interaction with an UDP tracker
 *
 */
public class UdpTracker implements ITracker {

	private static final int DEFAULT_SCRAPE_INTERVAL = 10000;

	private static final Logger LOGGER = LoggerFactory.getLogger(UdpTracker.class);

	private final TorrentClient torrentClient;

	/**
	 * The clock instance to obtain the time
	 */
	private Clock clock;

	private TrackerConnection connection;

	/**
	 * A hashmap containing all torrents which this tracker is supposed to know
	 */
	private Map<Torrent, TorrentInfo> torrentMap;

	/**
	 * The timestamp of the last scrape
	 */
	private LocalDateTime lastScrapeTime;

	/**
	 * The minimum announce interval as requested by the tracker
	 */
	private int announceInterval;

	/**
	 * The amount of tracker errors detected<br/>
	 * Successful operation decrease errorCount by 1 to a minimum of 0
	 */
	private int errorCount;

	public UdpTracker(String url, TorrentClient torrentClient) {
		this(url, torrentClient, Clock.systemDefaultZone());
	}

	public UdpTracker(String url, TorrentClient torrentClient, Clock clock) {
		this.torrentClient = torrentClient;
		this.clock = clock;
		connection = new TrackerConnection(url, torrentClient);
		torrentMap = new HashMap<>();
		announceInterval = (int) TorrentInfo.DEFAULT_ANNOUNCE_INTERVAL.toMillis();
		lastScrapeTime = LocalDateTime.now(clock).minus(DEFAULT_SCRAPE_INTERVAL, ChronoUnit.MILLIS);
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.ITracker#addTorrent(torrent.download.Torrent)
	 */
	@Override
	public void addTorrent(Torrent torrent) {
		if(!torrentMap.containsKey(torrent.getHash())) {
			synchronized (this) {
				torrentMap.put(torrent, new TorrentInfo(torrent, clock));
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
			for (TorrentInfo torrentInfo : torrentMap.values()) {
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
		if(Duration.between(lastScrapeTime, LocalDateTime.now(clock)).compareTo(Duration.of(DEFAULT_SCRAPE_INTERVAL, ChronoUnit.MILLIS)) < 0) {
			// We're not allowed to scrape yet
			return;
		}

		synchronized (this) {
			for (TorrentInfo torrentInfo : torrentMap.values()) {
				torrentClient.getExecutorService()
						.submit(new CallbackFuture<Void>(() -> {
							connection.scrape(Collections.singletonList(torrentInfo));
							return null;
						}, this::trackerCallback));
			}
		}
	}

	private void trackerCallback(Future<Void> task) {
		try {
			task.get();
			errorCount = Math.max(errorCount - 1, 0);
		} catch (Exception e) {
			LOGGER.warn("Request failed.", e);
			errorCount++;
		}
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.ITracker#announce(torrent.download.Torrent)
	 */
	@Override
	public void announce(Torrent torrent) {
		TorrentInfo torrentInfo = torrentMap.get(torrent);

		if(torrentInfo.getTimeSinceLastAnnouce().compareTo(Duration.of(announceInterval, ChronoUnit.MILLIS)) < 0) {
			// We're not allowed to announce yet
			return;
		}

		torrentClient.getExecutorService()
			.submit(new CallbackFuture<Void>(() -> {
				announceInterval = connection.announce(torrentInfo);
				torrentInfo.updateAnnounceTime();
				return null;
			}, this::trackerCallback));
	}

	public int getErrorCount() {
		return errorCount;
	}

	@Override
	public String getName() {
		return connection.getTrackerName();
	}

	@Override
	public String getStatus() {
		return connection.getStatus();
	}

}
