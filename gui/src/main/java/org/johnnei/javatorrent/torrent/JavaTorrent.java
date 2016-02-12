package org.johnnei.javatorrent.torrent;

import java.util.Optional;
import java.util.concurrent.Executors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.download.algos.PhaseMetadata;
import org.johnnei.javatorrent.download.algos.PhasePreMetadata;
import org.johnnei.javatorrent.magnetlink.MagnetLink;
import org.johnnei.javatorrent.network.protocol.ConnectionDegradation;
import org.johnnei.javatorrent.network.protocol.TcpSocket;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.protocol.messages.ut_metadata.UTMetadataExtension;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.algos.BurstPeerManager;
import org.johnnei.javatorrent.torrent.download.algos.PhaseData;
import org.johnnei.javatorrent.torrent.download.algos.PhaseSeed;
import org.johnnei.javatorrent.torrent.frame.TorrentFrame;
import org.johnnei.javatorrent.torrent.tracker.PeerConnectorPool;
import org.johnnei.javatorrent.utils.config.Config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaTorrent extends Thread {

	private static Logger LOGGER = LoggerFactory.getLogger(JavaTorrent.class);

	private static void loadDefaultConfig() {
		Config.getConfig().load();
		Config.getConfig().setDefault("peer-max", 500);
		Config.getConfig().setDefault("peer-max_burst_ratio", 1.5F);
		Config.getConfig().setDefault("peer-max_concurrent_connecting", 2);
		Config.getConfig().setDefault("peer-max_connecting", 50);
		Config.getConfig().setDefault("download-output_folder", ".\\");
		Config.getConfig().setDefault("download-port", 6881);
		Config.getConfig().setDefault("general-show_all_peers", false);
	}

	public static void main(String[] args) {
		loadDefaultConfig();

		try {
			TorrentClient torrentClient = new TorrentClient.Builder()
					.setConnectionDegradation(new ConnectionDegradation.Builder()
							.registerDefaultConnectionType(TcpSocket.class, TcpSocket::new, Optional.empty())
							.build())
					.registerModule(new ExtensionModule.Builder()
							.registerExtension(new UTMetadataExtension())
							.build())
					.setPhaseRegulator(new PhaseRegulator.Builder()
							.registerInitialPhase(PhasePreMetadata.class, PhasePreMetadata::new, Optional.of(PhaseMetadata.class))
							.registerPhase(PhaseMetadata.class, PhaseMetadata::new, Optional.of(PhaseData.class))
							.registerPhase(PhaseData.class, PhaseData::new, Optional.of(PhaseSeed.class))
							.registerPhase(PhaseSeed.class, PhaseSeed::new, Optional.empty())
							.build())
					.setPeerConnector(PeerConnectorPool::new)
					.setExecutorService(Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors() - 1)))
					.setPeerManager(new BurstPeerManager(Config.getConfig().getInt("peer-max"), Config.getConfig().getFloat("peer-max_burst_ratio")))
					.build();

			TorrentFrame frame = new TorrentFrame(torrentClient);
			boolean showGui = true;
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg.startsWith("magnet")) {
					MagnetLink magnet = new MagnetLink(arg, torrentClient);
					if (magnet.isDownloadable()) {
						Torrent torrent = magnet.getTorrent();
						torrent.start();
					} else {
						LOGGER.warn("Magnet link error occured");
					}
					frame.addTorrent(magnet.getTorrent());
				} else if (arg.startsWith("-no-gui")) {
					showGui = false;
				}
			}
			if (showGui) {
				frame.setVisible(true);
			} else {
				frame.dispose();
			}
		} catch (Exception e) {
			LOGGER.error("Failed to instantiate torrent client.", e);
		}
	}

}
