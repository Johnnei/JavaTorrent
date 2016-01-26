package org.johnnei.javatorrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.johnnei.javatorrent.bittorrent.module.IModule;
import org.johnnei.javatorrent.bittorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.network.protocol.ConnectionDegradation;

import torrent.TorrentManager;
import torrent.download.tracker.TrackerFactory;
import torrent.download.tracker.TrackerManager;
import torrent.protocol.MessageFactory;

/**
 * The Torrent Client is the main entry point for the configuration and initiation of downloads/uploads.
 *
 */
public class TorrentClient {

	private ConnectionDegradation connectionDegradation;

	private MessageFactory messageFactory;

	private TorrentManager torrentManager;

	private TrackerManager trackerManager;

	private Thread trackerManagerThread;

	private PhaseRegulator phaseRegulator;

	private TorrentClient(Builder builder) {
		connectionDegradation = Objects.requireNonNull(builder.connectionDegradation, "Connection degradation is required to setup connections with peers.");
		messageFactory = builder.messageFactoryBuilder.build();
		phaseRegulator = Objects.requireNonNull(builder.phaseRegulator, "Phase regulator is required to regulate the download/seed phases of a torrent.");

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

		private final MessageFactory.Builder messageFactoryBuilder;

		private PhaseRegulator phaseRegulator;

		private TrackerFactory trackerFactory;

		private final Collection<IModule> modules;

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

		public TorrentClient build() {
			return new TorrentClient(this);
		}

	}

}
