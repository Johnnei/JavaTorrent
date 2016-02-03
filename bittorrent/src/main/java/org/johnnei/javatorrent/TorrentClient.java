package org.johnnei.javatorrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Supplier;

import org.johnnei.javatorrent.bittorrent.module.IModule;
import org.johnnei.javatorrent.bittorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.network.protocol.ConnectionDegradation;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.TorrentManager;
import org.johnnei.javatorrent.torrent.download.algos.IPeerManager;
import org.johnnei.javatorrent.torrent.protocol.MessageFactory;
import org.johnnei.javatorrent.torrent.tracker.IPeerConnector;
import org.johnnei.javatorrent.torrent.tracker.ITracker;
import org.johnnei.javatorrent.torrent.tracker.TrackerException;
import org.johnnei.javatorrent.torrent.tracker.TrackerFactory;
import org.johnnei.javatorrent.torrent.tracker.TrackerManager;
import org.johnnei.javatorrent.utils.CheckedBiFunction;
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

	private IPeerManager peerManager;

	private ExecutorService executorService;

	private TorrentClient(Builder builder) {
		connectionDegradation = Objects.requireNonNull(builder.connectionDegradation, "Connection degradation is required to setup connections with peers.");
		LOGGER.info(String.format("Configured connection types: %s", connectionDegradation));
		messageFactory = builder.messageFactoryBuilder.build();
		phaseRegulator = Objects.requireNonNull(builder.phaseRegulator, "Phase regulator is required to regulate the download/seed phases of a torrent.");
		LOGGER.info(String.format("Configured phases: %s", phaseRegulator));
		executorService = Objects.requireNonNull(builder.executorService, "Executor service is required to process torrent tasks.");

		peerManager = Objects.requireNonNull(builder.peerManager, "Peer manager required to handle peer selection mechanism.");
		LOGGER.info(String.format("Configured %s as Peer Manager", peerManager));

		peerConnector = Objects.requireNonNull(builder.peerConnector.apply(this), "Peer connector required to allow external connections");
		LOGGER.info(String.format("Configured %s as Peer Connector", peerConnector));

		Objects.requireNonNull(builder.trackerFactoryBuilder, "At least one tracker protocol must be configured.");
		TrackerFactory trackerFactory = builder.trackerFactoryBuilder.setTorrentClient(this).build();

		torrentManager = new TorrentManager(this);
		trackerManager = new TrackerManager(peerConnector, trackerFactory);
		LOGGER.info(String.format("Configured trackers: %s", trackerFactory));

		LOGGER.info(String.format("Configured modules: %s", builder.modules.stream()
				.map(m -> String.format("%s (BEP %d)", m.getClass().getSimpleName(), m.getRelatedBep()))
				.reduce((a, b) -> a + ", " + b).orElse("")));
	}

	public void start() {
		torrentManager.startListener(trackerManager);
	}

	/**
	 * Gets the message factory for this client
	 * @return The {@link MessageFactory}
	 */
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

	/**
	 * Gets the {@link TrackerManager} which manages the collective instances {@link ITracker}
	 * @return
	 */
	public TrackerManager getTrackerManager() {
		return trackerManager;
	}

	/**
	 * Gets the {@link PhaseRegulator} which manages the ordering of the download states.
	 * @return
	 */
	public PhaseRegulator getPhaseRegulator() {
		return phaseRegulator;
	}

	/**
	 * Gets the {@link ExecutorService} which will execute the small tasks
	 * @return The executor service implementation
	 */
	public ExecutorService getExecutorService() {
		return executorService;
	}

	/**
	 * Gets the {@link IPeerConnector} which connects new peers
	 * @return The peer connector implementation
	 */
	public IPeerConnector getPeerConnector() {
		return peerConnector;
	}

	/**
	 * Gets the {@link IPeerManager} which handles the choking/unchoking of the connected peers.
	 * @return The peer manager implementation
	 */
	public IPeerManager getPeerManager() {
		return peerManager;
	}

	public static class Builder {

		private final MessageFactory.Builder messageFactoryBuilder;

		private final Collection<IModule> modules;

		private ConnectionDegradation connectionDegradation;

		private PhaseRegulator phaseRegulator;

		private TrackerFactory.Builder trackerFactoryBuilder;

		private Function<TorrentClient, IPeerConnector> peerConnector;

		private ExecutorService executorService;

		private IPeerManager peerManager;

		public Builder() {
			messageFactoryBuilder = new MessageFactory.Builder();
			trackerFactoryBuilder = new TrackerFactory.Builder();
			modules = new ArrayList<>();
		}

		public Builder registerModule(IModule module) {
			for (Class<IModule> dependingModule : module.getDependsOn()) {
				if (!modules.stream().anyMatch(m -> m.getClass().equals(dependingModule))) {
					throw new IllegalStateException(String.format("Depeding module %s is missing.", dependingModule.getSimpleName()));
				}
			}

			modules.add(module);
			return this;
		}

		/**
		 * Returns the list of reserved bits to enable to indicate that we support this extension.
		 * The bit numbers are represented in the following order: Right to left, starting at zero.
		 * For reference see BEP 10 which indicates that bit 20 must be enabled.
		 * @param bit The bit to enable.
		 */
		public void enableExtensionBit(int bit) {
			// TODO Implement
		}

		public Builder registerMessage(int id, Supplier<IMessage> messageSupplier) {
			messageFactoryBuilder.registerMessage(id, messageSupplier);
			return this;
		}

		public Builder registerTrackerProtocol(String protocol, CheckedBiFunction<String, TorrentClient, ITracker, TrackerException> supplier) {
			trackerFactoryBuilder.registerProtocol(protocol, supplier);
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

		public Builder setPeerConnector(Function<TorrentClient, IPeerConnector> peerConnector) {
			this.peerConnector = peerConnector;
			return this;
		}

		public Builder setExecutorService(ExecutorService executorService) {
			this.executorService = executorService;
			return this;
		}

		public Builder setPeerManager(IPeerManager peerManager) {
			this.peerManager = peerManager;
			return this;
		}

		public TorrentClient build() {
			return new TorrentClient(this);
		}

	}

}
