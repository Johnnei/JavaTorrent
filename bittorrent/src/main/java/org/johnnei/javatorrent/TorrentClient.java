package org.johnnei.javatorrent;

import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.network.protocol.ConnectionDegradation;
import org.johnnei.javatorrent.network.protocol.TcpSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import torrent.TorrentManager;
import torrent.download.tracker.TrackerFactory;
import torrent.download.tracker.TrackerManager;
import torrent.protocol.BitTorrent;
import torrent.protocol.MessageFactory;
import torrent.protocol.messages.MessageBitfield;
import torrent.protocol.messages.MessageBlock;
import torrent.protocol.messages.MessageCancel;
import torrent.protocol.messages.MessageChoke;
import torrent.protocol.messages.MessageHave;
import torrent.protocol.messages.MessageInterested;
import torrent.protocol.messages.MessageRequest;
import torrent.protocol.messages.MessageUnchoke;
import torrent.protocol.messages.MessageUninterested;

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

	private Thread trackerManagerThread;

	private PhaseRegulator phaseRegulator;

	private TorrentClient(Builder builder) {
		connectionDegradation = builder.connectionDegradation;
		messageFactory = builder.messageFactoryBuilder.build();
		phaseRegulator = builder.phaseRegulator;

		torrentManager = new TorrentManager(this);
		trackerManager = new TrackerManager(this, builder.trackerFactory);

		trackerManagerThread = new Thread(trackerManager, "Tracker manager");
		trackerManagerThread.setDaemon(true);
	}

	public void start() {
		trackerManagerThread.start();
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

	public static class Builder {

		private ConnectionDegradation connectionDegradation;

		private MessageFactory.Builder messageFactoryBuilder;

		private PhaseRegulator phaseRegulator;

		private TrackerFactory trackerFactory;

		/**
		 * Creates a builder with all default modules configured.
		 */
		public static Builder createDefaultBuilder() {
			LOGGER.debug("Configuring connection degradation to only support TCP");
			Builder builder = new Builder()
					.setConnectionDegradation(new ConnectionDegradation.Builder()
							.registerDefaultConnectionType(TcpSocket.class, () -> new TcpSocket(), Optional.empty())
							.build());

			return builder;
		}

		public Builder() {
			messageFactoryBuilder = new MessageFactory.Builder()
				// Register BitTorrent messages
				.registerMessage(BitTorrent.MESSAGE_BITFIELD, () -> new MessageBitfield())
				.registerMessage(BitTorrent.MESSAGE_CANCEL, () -> new MessageCancel())
				.registerMessage(BitTorrent.MESSAGE_CHOKE, () -> new MessageChoke())
				.registerMessage(BitTorrent.MESSAGE_HAVE, () -> new MessageHave())
				.registerMessage(BitTorrent.MESSAGE_INTERESTED, () -> new MessageInterested())
				.registerMessage(BitTorrent.MESSAGE_PIECE, () -> new MessageBlock())
				.registerMessage(BitTorrent.MESSAGE_REQUEST, () -> new MessageRequest())
				.registerMessage(BitTorrent.MESSAGE_UNCHOKE, () -> new MessageUnchoke())
				.registerMessage(BitTorrent.MESSAGE_UNINTERESTED, () -> new MessageUninterested());
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

		public TorrentClient build() {
			return new TorrentClient(this);
		}

	}

}
