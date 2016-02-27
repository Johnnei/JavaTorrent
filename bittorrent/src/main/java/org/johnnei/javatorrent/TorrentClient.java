package org.johnnei.javatorrent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;

import org.johnnei.javatorrent.bittorrent.protocol.MessageFactory;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerFactory;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.internal.torrent.TorrentManager;
import org.johnnei.javatorrent.torrent.algos.peermanager.IPeerManager;
import org.johnnei.javatorrent.tracker.IPeerConnector;
import org.johnnei.javatorrent.internal.tracker.TrackerManager;
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

	private int downloadPort;

	private final byte[] extensionBytes;

	private final byte[] peerId;

	private AtomicInteger transactionId;

	private TorrentClient(Builder builder) {
		connectionDegradation = Objects.requireNonNull(builder.connectionDegradation, "Connection degradation is required to setup connections with peers.");
		LOGGER.info(String.format("Configured connection types: %s", connectionDegradation));
		messageFactory = builder.messageFactoryBuilder.build();
		phaseRegulator = Objects.requireNonNull(builder.phaseRegulator, "Phase regulator is required to regulate the download/seed phases of a torrent.");
		LOGGER.info(String.format("Configured phases: %s", phaseRegulator));
		executorService = Objects.requireNonNull(builder.executorService, "Executor service is required to process torrent tasks.");

		peerManager = Objects.requireNonNull(builder.peerManager, "Peer manager required to handle peer selection mechanism.");
		LOGGER.info(String.format("Configured %s as Peer Manager", peerManager));

		peerConnector = Objects.requireNonNull(builder.peerConnector, "Peer connector required to allow external connections").apply(this);
		LOGGER.info(String.format("Configured %s as Peer Connector", peerConnector));

		Objects.requireNonNull(builder.trackerFactoryBuilder, "At least one tracker protocol must be configured.");
		TrackerFactory trackerFactory = builder.trackerFactoryBuilder.setTorrentClient(this).build();

		torrentManager = new TorrentManager(this);
		trackerManager = new TrackerManager(peerConnector, trackerFactory);
		LOGGER.info(String.format("Configured trackers: %s", trackerFactory));

		LOGGER.info(String.format("Configured modules: %s", builder.modules.stream()
				.map(m -> String.format("%s (BEP %d)", m.getClass().getSimpleName(), m.getRelatedBep()))
				.reduce((a, b) -> a + ", " + b).orElse("")));

		downloadPort = builder.downloadPort;
		extensionBytes = builder.extensionBytes;
		peerId = createPeerId();
		transactionId = new AtomicInteger(new Random().nextInt());

		torrentManager.startListener();
	}

	private byte[] createPeerId() {
		char[] version = Version.BUILD.split(" ")[1].replace(".", "").toCharArray();
		byte[] peerId = new byte[20];
		peerId[0] = '-';
		peerId[1] = 'J';
		peerId[2] = 'T';
		peerId[3] = (byte) version[0];
		peerId[4] = (byte) version[1];
		peerId[5] = (byte) version[2];
		peerId[6] = (byte) version[3];
		peerId[7] = '-';

		Random random = new Random();
		for (int i = 8; i < peerId.length; i++) {
			peerId[i] = (byte) (random.nextInt() & 0xFF);
		}
		return peerId;
	}

	/**
	 * Initiates the downloading of a torrent.
	 * @param torrent The torrent to download.
	 * @param trackerUrls The trackers which are known for this torrent.
	 */
	public void download(Torrent torrent, Collection<String> trackerUrls) {
		trackerUrls.forEach(url -> trackerManager.addTorrent(torrent, url));
		download(torrent);
	}

	/**
	 * Initiates the downloading of a torrent.
	 * @param torrent The torrent to download.
	 */
	public void download(Torrent torrent) {
		torrentManager.addTorrent(torrent);
		torrent.start();
	}

	public int createUniqueTransactionId() {
		return transactionId.incrementAndGet();
	}

	/**
	 * Calculates how many connections are assigned to the torrent but haven't passed the BitTorrent handshake yet.
	 * @param torrent The torrent for which connections must be counted.
	 * @return The amount of pending peers.
	 */
	public int getConnectingCountFor(Torrent torrent) {
		return trackerManager.getConnectingCountFor(torrent);
	}

	/**
	 * Gets all trackers which know the given torrent
	 * @param torrent the torrent which the tracker must support
	 * @return a collection of trackers which support the given torrent
	 */
	public List<ITracker> getTrackersFor(Torrent torrent) {
		return trackerManager.getTrackersFor(torrent);
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

	/**
	 * Gets the {@link PhaseRegulator} which manages the ordering of the download states.
	 * @return The configured phase regulator.
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

	/**
	 * Gets the port at which we are listening for peers
	 * @return The port at which we are listening
	 */
	public int getDownloadPort() {
		return downloadPort;
	}

	/**
	 * Gets the eight extension bytes which represent which BitTorrent extensions are enabled on this client.
	 * @return The extension bytes
	 */
	public byte[] getExtensionBytes() {
		return Arrays.copyOf(extensionBytes, extensionBytes.length);
	}

	/**
	 * Gets the peer ID associated to this tracker manager
	 *
	 * @return The peer ID
	 */
	public byte[] getPeerId() {
		return peerId;
	}

	/**
	 * Gets the torrent associated with the given hash.
	 * @param torrentHash The BTIH of the torrent
	 * @return The torrent if known.
	 */
	public Optional<Torrent> getTorrentByHash(byte[] torrentHash) {
		return torrentManager.getTorrent(torrentHash);
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

		private int downloadPort;

		private byte[] extensionBytes;

		public Builder() {
			messageFactoryBuilder = new MessageFactory.Builder();
			trackerFactoryBuilder = new TrackerFactory.Builder();
			modules = new ArrayList<>();
			extensionBytes = new byte[8];
		}

		public Builder registerModule(IModule module) {
			for (Class<IModule> dependingModule : module.getDependsOn()) {
				if (!modules.stream().anyMatch(m -> m.getClass().equals(dependingModule))) {
					throw new IllegalStateException(String.format("Depending module %s is missing.", dependingModule.getSimpleName()));
				}
			}

			module.configureTorrentClient(this);
			modules.add(module);
			return this;
		}

		/**
		 * Enables a bit in the extension bytes. According to BEP 3 there are 8 extension bytes (reserved bytes).
		 * The bit numbers are represented in the following order: Right to left, starting at zero.
		 * For reference see BEP 10 which indicates that bit 20 must be enabled.
		 * @param bit The bit to enable.
		 */
		public Builder enableExtensionBit(int bit) {
			final int index = (extensionBytes.length - 1 - (bit / 8));
			final int bitValue = 1 << Math.floorMod(bit, 8);

			extensionBytes[index] |= bitValue;
			return this;
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

		/**
		 * Sets the download port at which we are listening
		 * @param downloadPort The port at which we are listening
		 * @return The modified instance
		 */
		public Builder setDownloadPort(int downloadPort) {
			this.downloadPort = downloadPort;
			return this;
		}

		public TorrentClient build() throws Exception {
			TorrentClient client = new TorrentClient(this);
			for (IModule module : modules) {
				module.onBuild(client);
			}
			return client;
		}

	}

}
