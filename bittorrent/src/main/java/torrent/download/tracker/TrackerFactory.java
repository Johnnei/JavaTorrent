package torrent.download.tracker;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import torrent.download.Torrent;

public class TrackerFactory {

	private static final Logger LOGGER = LoggerFactory.getLogger(TrackerFactory.class);

	/**
	 * The cache of already created tracker instances for urls
	 */
	private final Map<String, ITracker> trackerInstances;

	private final Map<String, BiFunction<String, IPeerConnector, ITracker>> trackerSuppliers;

	private final IPeerConnector peerConnector;

	private TrackerFactory(Builder builder) {
		trackerSuppliers = builder.trackerSuppliers;
		peerConnector = builder.peerConnector;
		trackerInstances = new HashMap<>();
	}

	/**
	 * Either creates or returns the tracker implementation for the given url
	 * @param trackerUrl The url (including protocol) at which the tracker is available
	 * @return The tracker instance which handles the connection to the given url
	 *
	 * @throws IllegalArgumentException When the given tracker URL doesn't contain a protocol definition or an incomplete definition.
	 */
	public ITracker getTrackerFor(String trackerUrl) {
		if (trackerInstances.containsKey(trackerUrl)) {
			return trackerInstances.get(trackerUrl);
		}

		if (!trackerUrl.contains("://")) {
			throw new IllegalArgumentException(String.format("Missing protocol definition in: %s", trackerUrl));
		}

		String[] trackerParts = trackerUrl.split("://", 2);
		final String protocol = trackerParts[0];
		if (!trackerSuppliers.containsKey(protocol)) {
			throw new IllegalArgumentException(String.format("Unsupported protocol: %s", protocol));
		}

		return trackerSuppliers.get(protocol).apply(trackerUrl, peerConnector);
	}

	public Collection<ITracker> getTrackingsHavingTorrent(Torrent torrent) {
		return trackerInstances.values().stream()
				.filter(tracker -> tracker.hasTorrent(torrent))
				.collect(Collectors.toList());
	}

	public static class Builder {

		private Map<String, BiFunction<String, IPeerConnector, ITracker>> trackerSuppliers;

		private IPeerConnector peerConnector;

		public Builder registerProtocol(String protocol, BiFunction<String, IPeerConnector, ITracker> supplier) {
			if (trackerSuppliers.containsKey(protocol)) {
				LOGGER.warn(String.format("Overriding existing %s protocol implementation", protocol));
			}

			trackerSuppliers.put(protocol, supplier);

			return this;
		}

		public Builder setPeerConnector(IPeerConnector peerConnector) {
			this.peerConnector = peerConnector;
			return this;
		}

		public TrackerFactory build() {
			return new TrackerFactory(this);
		}

	}

}
