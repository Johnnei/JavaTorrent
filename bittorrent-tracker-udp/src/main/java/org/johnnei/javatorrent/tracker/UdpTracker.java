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
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.tracker.udp.AnnounceRequest;
import org.johnnei.javatorrent.internal.tracker.udp.Connection;
import org.johnnei.javatorrent.internal.tracker.udp.IUdpTrackerPayload;
import org.johnnei.javatorrent.internal.tracker.udp.ScrapeRequest;
import org.johnnei.javatorrent.internal.tracker.udp.UdpTrackerSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
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

	private final String url;

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

	protected UdpTracker(Builder builder) throws TrackerException {
		this.url = Objects.requireNonNull(builder.trackerUrl, "Tracker URL must be given");
		this.torrentClient = builder.torrentClient;
		this.clock = builder.clock;
		this.trackerSocket = builder.socket;

		activeConnection = new Connection(clock);
		torrentMap = new HashMap<>();
		announceInterval = (int) TorrentInfo.DEFAULT_ANNOUNCE_INTERVAL.toMillis();
		lastScrapeTime = LocalDateTime.now(clock).minus(DEFAULT_SCRAPE_INTERVAL, ChronoUnit.MILLIS);

		// Parse URL
		Pattern regex = Pattern.compile("udp://([^:]+):(\\d+)");
		Matcher matcher = regex.matcher(builder.trackerUrl);

		if (!matcher.matches()) {
			throw new TrackerException(String.format("Tracker url doesn't match the expected format. URL: %s", builder.trackerUrl));
		}

		try {
			name = matcher.group(1);
			InetAddress address = InetAddress.getByName(matcher.group(1));
			int port = Integer.parseInt(matcher.group(2));
			trackerAddress = new InetSocketAddress(address, port);
			status = STATE_IDLE;
		} catch (Exception e) {
			name = "Unknown";
			status = STATE_CRASHED;
			LOGGER.warn(String.format("Failed to resolve tracker: %s", builder.trackerUrl), e);
		}
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.ITracker#addTorrent(torrent.download.Torrent)
	 */
	@Override
	public void addTorrent(Torrent torrent) {
		synchronized (this) {
			if(!torrentMap.containsKey(torrent)) {
				torrentMap.put(torrent, new TorrentInfo(torrent, clock));
			}
		}
	}

	/* (non-Javadoc)
	 * @see torrent.download.tracker.ITracker#hasTorrent(torrent.download.Torrent)
	 */
	@Override
	public boolean hasTorrent(Torrent torrent) {
		synchronized (this) {
			return torrentMap.containsKey(torrent);
		}
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
		TorrentInfo torrentInfo;

		synchronized (this) {
			torrentInfo = torrentMap.get(torrent);
		}

		if(torrentInfo.getTimeSinceLastAnnouce().compareTo(Duration.of(announceInterval, ChronoUnit.MILLIS)) < 0) {
			// We're not allowed to announce yet
			return;
		}

		trackerSocket.submitRequest(this, new AnnounceRequest(torrentInfo, torrentClient.getTrackerManager().getPeerId(), torrentClient.getDownloadPort()));
	}

	/**
	 * Gets called by the underlying {@link #trackerSocket} if a request caused {@link #announce(Torrent)} or {@link #scrape()} fails.
	 * @param payload
	 */
	public void onRequestFailed(IUdpTrackerPayload payload) {
		LOGGER.warn("Failed to execute {}.", payload);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public String toString() {
		return "UdpTracker [name=" + name + ", status=" + status + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + url.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (!(obj instanceof UdpTracker)) {
			return false;
		}

		UdpTracker other = (UdpTracker) obj;
		if (Objects.equals(url, other.url)) {
			return true;
		}

		return false;
	}

	/**
	 * Updates the connection
	 * @param connection
	 */
	public void setConnection(Connection connection) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("Received new connection: %s", connection));
		}
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

	public static final class Builder {

		private Clock clock;

		private UdpTrackerSocket socket;

		private String trackerUrl;

		private TorrentClient torrentClient;

		public Builder() {
			clock = Clock.systemDefaultZone();
		}

		public Builder setTorrentClient(TorrentClient torrentClient) {
			this.torrentClient = torrentClient;
			return this;
		}

		/**
		 * Sets the clock to use as time reference. By default {@link Clock#systemDefaultZone()}.
		 * @param clock
		 * @return
		 */
		public Builder setClock(Clock clock) {
			this.clock = clock;
			return this;
		}

		public Builder setSocket(UdpTrackerSocket socket) {
			this.socket = socket;
			return this;
		}

		public Builder setUrl(String trackerUrl) {
			this.trackerUrl = trackerUrl;
			return this;
		}

		public UdpTracker build() throws TrackerException {
			return new UdpTracker(this);
		}

	}

}
