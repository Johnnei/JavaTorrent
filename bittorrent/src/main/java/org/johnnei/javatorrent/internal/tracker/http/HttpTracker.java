package org.johnnei.javatorrent.internal.tracker.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedList;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.Bencoding;
import org.johnnei.javatorrent.bittorrent.encoding.IBencodedValue;
import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.utils.Argument;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ITracker} by using the HTTP variant of the Tracker protocol as defined in BEP #3.
 */
public class HttpTracker implements ITracker {

	private static final Logger LOGGER = LoggerFactory.getLogger(HttpTracker.class);

	private static final String STATE_ANNOUNCING = "Announcing";

	private static final String STATE_ANNOUNCE_ERROR = "Announce failed";

	private static final String STATE_IDLE = "Idle";

	private final Clock clock = Clock.systemDefaultZone();

	private final TorrentClient torrentClient;

	private final TrackerUrl trackerUrl;

	private final OkHttpClient httpClient;

	private final Bencoding bencoding;

	private Map<Torrent, TorrentInfo> torrentMap;

	private long announceInterval = 30_000;

	private String status;

	public HttpTracker(Builder builder) {
		torrentClient = Argument.requireNonNull(builder.torrentClient, "Torrent Client must be provided");
		trackerUrl = new TrackerUrl(Argument.requireNonNull(builder.trackerUrl, "Tracker Url must be provided"));
		httpClient = new OkHttpClient();
		torrentMap = new HashMap<>();
		bencoding = new Bencoding();
		status = STATE_IDLE;
	}

	@Override
	public void announce(Torrent torrent) {
		TorrentInfo torrentInfo;

		synchronized (this) {
			torrentInfo = torrentMap.get(torrent);
		}

		if(torrentInfo.getTimeSinceLastAnnounce().compareTo(Duration.of(announceInterval, ChronoUnit.MILLIS)) < 0) {
			// We're not allowed to announce yet
			return;
		}

		HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
				.scheme(trackerUrl.getSchema())
				.host(trackerUrl.getHost())
				.port(trackerUrl.getPort())
				.addPathSegments(trackerUrl.getPath())
				.addEncodedQueryParameter("info_hash", hexEncode(torrent.getHashArray()))
				.addEncodedQueryParameter("peer_id", hexEncode(torrentClient.getPeerId()))
				.addQueryParameter("port", Integer.toUnsignedString(torrentClient.getDownloadPort()))
				// TODO Add support for IP field
				.addQueryParameter("uploaded", Long.toString(torrent.getUploadedBytes()))
				.addQueryParameter("downloaded", Long.toString(torrent.getDownloadedBytes()))
				// TODO Add support for BEP-23, for now enforce non-compact results.
				.addQueryParameter("compact", "0");


		if (torrent.getFileSet() != null) {
			urlBuilder.addQueryParameter("left", Long.toString(torrent.getFileSet().countRemainingBytes()));
		} else {
			// We don't know how much we need to download yet.
			urlBuilder.addQueryParameter("left", "0");
		}

		TorrentInfo info = torrentMap.get(torrent);
		TrackerEvent event = info.getEvent();
		if (event != TrackerEvent.EVENT_NONE) {
			info.setEvent(TrackerEvent.EVENT_NONE);

			urlBuilder.addQueryParameter("event", event.getTextual());
		}

		Request request = new Request.Builder().url(urlBuilder.build()).build();

		torrentClient.getExecutorService().submit(() -> doAnnounce(torrent, request));
	}

	private void doAnnounce(Torrent torrent, Request request) {
		try {
			status = STATE_ANNOUNCING;
			LOGGER.debug("Sending query: {}", request);
			Response response = httpClient.newCall(request).execute();

			InStream inStream = new InStream(response.body().bytes());
			BencodedMap result = (BencodedMap) bencoding.decode(inStream);
			LOGGER.debug("Query response: {}", result.asMap());

			Optional<IBencodedValue> failure = result.get("failure reason");
			if (failure.isPresent()) {
				status = STATE_ANNOUNCE_ERROR;
				LOGGER.error("Tracker returned \"{}\" for announce: {}", failure.get(), request);
				return;
			}

			result.get("interval").ifPresent(interval -> announceInterval = interval.asLong());
			result.get("peers").ifPresent(peers -> processPeers(torrent, peers));

			status = STATE_IDLE;
		} catch (IOException e) {
			status = STATE_ANNOUNCE_ERROR;
			LOGGER.warn("Announce failed with error", e);
		}
	}

	private void processPeers(Torrent torrent, IBencodedValue peers) {
		if (!(peers instanceof BencodedList)) {
			LOGGER.warn(String.format(
					"Tracker \"%s\" returned peers list in an unsupported format. (Most likely BEP #23 is not configured).",
					trackerUrl.getHost()));
			return;
		}

		peers.asList().stream().map(entry -> (BencodedMap) entry).forEach(peer -> {
			LOGGER.debug("Received peer: {}", peer);

			Optional<IBencodedValue> hostname = peer.get("ip");
			Optional<IBencodedValue> port = peer.get("port");

			if (!hostname.isPresent() || !port.isPresent()) {
				LOGGER.warn(String.format("Tracker \"%s\" returned incomplete peer entry.", trackerUrl.getHost()));
				return;
			}

			PeerConnectInfo connectInfo = new PeerConnectInfo(
					torrent,
					InetSocketAddress.createUnresolved(hostname.get().asString(), (int) port.get().asLong()));
			connectPeer(connectInfo);
		});
	}

	@Override
	public void scrape() {
		throw new UnsupportedOperationException("HTTP trackers do not support scraping");
	}

	private String hexEncode(byte[] bytes) {
		StringBuilder stringBuilder = new StringBuilder();
		for (byte b : bytes) {
			stringBuilder.append("%");
			String encoded = Integer.toUnsignedString(Byte.toUnsignedInt(b), 16);
			if (encoded.length() == 1) {
				stringBuilder.append("0");
			}

			stringBuilder.append(encoded);
		}
		return stringBuilder.toString();
	}

	@Override
	public synchronized void addTorrent(Torrent torrent) {
		if(!torrentMap.containsKey(torrent)) {
			torrentMap.put(torrent, new TorrentInfo(torrent, clock));
		}
	}

	@Override
	public synchronized boolean hasTorrent(Torrent torrent) {
		return torrentMap.containsKey(torrent);
	}

	/**
	 * Finds the torrent info associated to the given torrent
	 * @param torrent The torrent on which we want info on
	 * @return The info or null if not found
	 */
	@Override
	public synchronized Optional<TorrentInfo> getInfo(Torrent torrent) {
		return Optional.ofNullable(torrentMap.get(torrent));
	}

	@Override
	public void connectPeer(PeerConnectInfo peer) {
		torrentClient.getPeerConnector().enqueuePeer(peer);
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public String getName() {
		return trackerUrl.getHost();
	}

	public long getAnnounceInterval() {
		return announceInterval;
	}

	/**
	 * A builder to create {@link HttpTracker} instances.
	 */
	public static final class Builder {

		private TorrentClient torrentClient;

		private String trackerUrl;

		/**
		 * Sets the {@link TorrentClient} for which the tracker which will receiving peers.
		 * @param torrentClient The client.
		 * @return The modified builder state.
		 */
		public Builder setTorrentClient(TorrentClient torrentClient) {
			this.torrentClient = torrentClient;
			return this;
		}

		/**
		 * Sets the tracker url at which the tracker is expected to be available.
		 * @param trackerUrl Url at which the tracker is expected to be available.
		 * @return The modified builder state.
		 */
		public Builder setUrl(String trackerUrl) {
			this.trackerUrl = trackerUrl;
			return this;
		}

		/**
		 * @return The newly created {@link HttpTracker} instance.
		 */
		public HttpTracker build() {
			return new HttpTracker(this);
		}
	}
}
