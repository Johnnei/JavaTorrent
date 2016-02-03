package org.johnnei.javatorrent.tracker;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.PeerConnectInfo;
import org.johnnei.javatorrent.torrent.tracker.ITracker;
import org.johnnei.javatorrent.torrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.torrent.tracker.TrackerException;
import org.johnnei.javatorrent.tracker.udp.AnnounceRequest;
import org.johnnei.javatorrent.tracker.udp.Connection;
import org.johnnei.javatorrent.tracker.udp.ScrapeRequest;
import org.johnnei.javatorrent.tracker.udp.UdpTrackerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages the interaction with an UDP tracker
 *
 */
public class UdpTracker implements ITracker {

	private static final int DEFAULT_SCRAPE_INTERVAL = 10000;

	public static final String STATE_ANNOUNCING = "Announcing";

	public static final String STATE_SCRAPING = "Scraping";

	public static final String STATE_CONNECTING = "Connecting";

	public static final String STATE_IDLE = "Idle";

	public static final String STATE_CRASHED = "Invalid tracker";

	private static final Logger LOGGER = LoggerFactory.getLogger(UdpTracker.class);

	/**
	 * The torrent client which created this tracker
	 */
	private final TorrentClient torrentClient;

	/**
	 * The clock instance to obtain the time
	 */
	private Clock clock;

	/**
	 * The UDP socket wrapper to interact with the tracker
	 */
	private UdpTrackerSocket trackerSocket;

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
	 * The endpoint at which the tracker is available
	 */
	private InetSocketAddress trackerAddress;

	/**
	 * The current connection with the tracker
	 */
	private Connection activeConnection;

	/**
	 * The display name for the tracker
	 */
	private String name;

	/**
	 * The current status
	 */
	private String status;

	public UdpTracker(TorrentClient torrentClient, UdpTrackerSocket trackerSocket, String url) throws TrackerException {
		this(torrentClient, trackerSocket, url, Clock.systemDefaultZone());
	}

	public UdpTracker(TorrentClient torrentClient, UdpTrackerSocket trackerSocket, String url, Clock clock) throws TrackerException {
		this.torrentClient = torrentClient;
		this.clock = clock;
		this.trackerSocket = trackerSocket;

		torrentMap = new HashMap<>();
		announceInterval = (int) TorrentInfo.DEFAULT_ANNOUNCE_INTERVAL.toMillis();
		lastScrapeTime = LocalDateTime.now(clock).minus(DEFAULT_SCRAPE_INTERVAL, ChronoUnit.MILLIS);

		// Parse URL
		Pattern regex = Pattern.compile("udp://([^:]+):(\\d+)");
		Matcher matcher = regex.matcher(url);

		if (!matcher.matches()) {
			throw new TrackerException(String.format("Tracker url doesn't match the expected format. URL: %s", url));
		}

		try {
			InetAddress address = InetAddress.getByName(matcher.group(1));
			int port = Integer.parseInt(matcher.group(2));
			trackerAddress = new InetSocketAddress(address, port);
			status = STATE_IDLE;
		} catch (Exception e) {
			name = "Unknown";
			status = STATE_CRASHED;
			LOGGER.warn(String.format("Failed to resolve tracker: %s", url), e);
		}
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
	public Optional<TorrentInfo> getInfo(Torrent torrent) {
		synchronized (this) {
			return Optional.ofNullable(torrentMap.get(torrent));
		}
	}

	@Override
	public void connectPeer(PeerConnectInfo peer) {
		torrentClient.getPeerConnector().connectPeer(peer);
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
			torrentMap.values().stream()
					.map(torrentInfo -> torrentInfo.getTorrent())
					.forEach(torrent -> trackerSocket.submitRequest(this, new ScrapeRequest(Collections.singletonList(torrent))));
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

		trackerSocket.submitRequest(this, new AnnounceRequest(torrentInfo, torrentClient.getTrackerManager().getPeerId()));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getStatus() {
		return status;
	}

	/**
	 * Updates the connection
	 * @param connection
	 */
	public void setConnection(Connection connection) {
		this.activeConnection = connection;
	}

	public Connection getConnection() {
		return activeConnection;
	}

	public void setAnnounceInterval(int interval) {
		announceInterval = interval;
	}

	public InetSocketAddress getSocketAddress() {
		return trackerAddress;
	}

}
