package org.johnnei.javatorrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.johnnei.javatorrent.bittorrent.module.IModule;
import org.johnnei.javatorrent.bittorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.network.protocol.ConnectionDegradation;
import org.johnnei.javatorrent.torrent.TorrentManager;
import org.johnnei.javatorrent.torrent.download.tracker.IPeerConnector;
import org.johnnei.javatorrent.torrent.download.tracker.TrackerFactory;
import org.johnnei.javatorrent.torrent.download.tracker.TrackerManager;
import org.johnnei.javatorrent.torrent.protocol.MessageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Torrent Client is the main entry point for the configuration and initiation of downloads/uploads.
 *
 */
public class TorrentClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(TorrentClient.class);

	private ConnectionDegradation connectionDegradation;

	private MessageFactory messageFactory;

	private TorrentManager torrentManager;

	private TrackerManager trackerManager;

	private PhaseRegulator phaseRegulator;

	private IPeerConnector peerConnector;

	private ExecutorService executorService;

	private TorrentClient(Builder builder) {
		connectionDegradation = Objects.requireNonNull(builder.connectionDegradation, "Connection degradation is required to setup connections with peers.");
		LOGGER.info(String.format("Configured connection types: %s", connectionDegradation));
		messageFactory = builder.messageFactoryBuilder.build();
		phaseRegulator = Objects.requireNonNull(builder.phaseRegulator, "Phase regulator is required to regulate the download/seed phases of a torrent.");
		LOGGER.info(String.format("Configured phases: %s", phaseRegulator));
		executorService = Objects.requireNonNull(builder.executorService, "Executor service is required to process torrent tasks.");

		peerConnector = Objects.requireNonNull(builder.peerConnector.apply(this), "Peer connector required to allow external connections");
		LOGGER.info(String.format("Configured %s as Peer Connector", peerConnector));

		torrentManager = new TorrentManager(this);
		trackerManager = new TrackerManager(peerConnector, Objects.requireNonNull(builder.trackerFactory, "At least one tracker protocol must be configured."));
		LOGGER.info(String.format("Configured trackers: %s", builder.trackerFactory));

		LOGGER.info(String.format("Configured modules: %s", builder.modules.stream()
				.map(m -> String.format("%s (BEP %d)", m.getClass().getSimpleName(), m.getRelatedBep()))
				.reduce((a, b) -> a + ", " + b).orElse("")));
	}

	public void start() {
		torrentManager.startListener(trackerManager);
	}

	public MessageFactory getMessageFactory() {
		return messageFactory;
	}

	/**
	 * Gets the configured connection degradation rules.
	 * @return The socket degradation rules.
	 */
	public ConnectionDegradation getConnectionDegradation() {
		return connectionDegradation;
	}

	public TorrentManager getTorrentManager() {
		return torrentManager;
	}

	public TrackerManager getTrackerManager() {
		return trackerManager;
	}

	public PhaseRegulator getPhaseRegulator() {
		return phaseRegulator;
	}

	public ExecutorService getExecutorService() {
		return executorService;
	}

	public static class Builder {

		private ConnectionDegradation connectionDegradation;

		private final MessageFactory.Builder messageFactoryBuilder;

		private PhaseRegulator phaseRegulator;

		private TrackerFactory trackerFactory;

		private final Collection<IModule> modules;

		private Function<TorrentClient, IPeerConnector> peerConnector;

		private ExecutorService executorService;

		public Builder() {
			messageFactoryBuilder = new MessageFactory.Builder();
			modules = new ArrayList<>();
		}

		public Builder registerModule(IModule module) {
			for (Class<IModule> dependingModule : module.getDependsOn()) {
				if (!modules.stream().anyMatch(m -> m.getClass().equals(dependingModule))) {
					throw new IllegalStateException(String.format("Depeding module %s is missing.", dependingModule.getSimpleName()));
				}
			}

			modules.add(module);
			module.getMessages().forEach(messageFactoryBuilder::registerMessage);

			// TODO Enable the reserved bits
			return this;
		}

		public Builder setPhaseRegulator(PhaseRegulator phaseRegulator) {
			this.phaseRegulator = phaseRegulator;
			return this;
		}

		public Builder setConnectionDegradation(ConnectionDegradation connectionDegradation) {
			this.connectionDegradation = connectionDegradation;
			return this;
		}

		public Builder setTrackerFactory(TrackerFactory trackerFactory) {
			this.trackerFactory = trackerFactory;
			return this;
		}

		public Builder setPeerConnector(Function<TorrentClient, IPeerConnector> peerConnector) {
			this.peerConnector = peerConnector;
			return this;
		}

		public Builder setExecutorService(ExecutorService executorService) {
			this.executorService = executorService;
			return this;
		}

		public TorrentClient build() {
			return new TorrentClient(this);
		}

	}

}
