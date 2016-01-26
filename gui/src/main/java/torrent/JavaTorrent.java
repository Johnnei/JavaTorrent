package torrent;

import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.download.algos.PhaseMetadata;
import org.johnnei.javatorrent.download.algos.PhasePreMetadata;
import org.johnnei.javatorrent.network.protocol.ConnectionDegradation;
import org.johnnei.javatorrent.network.protocol.TcpSocket;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.protocol.messages.ut_metadata.UTMetadataExtension;
import org.johnnei.utils.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import torrent.download.MagnetLink;
import torrent.download.Torrent;
import torrent.download.algos.PhaseData;
import torrent.download.algos.PhaseUpload;
import torrent.frame.TorrentFrame;

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
						.registerPhase(PhaseData.class, PhaseData::new, Optional.of(PhaseUpload.class))
						.registerPhase(PhaseUpload.class, PhaseUpload::new, Optional.empty())
						.build())
				.build();

		TorrentFrame frame= new TorrentFrame(torrentClient);
		boolean showGui = true;
		for(int i = 0; i < args.length; i++) {
			String arg = args[i];
			if(arg.startsWith("magnet")) {
				MagnetLink magnet = new MagnetLink(arg, torrentClient);
				if (magnet.isDownloadable()) {
					Torrent torrent = magnet.getTorrent();
					torrent.start();
				} else {
					LOGGER.warn("Magnet link error occured");
				}
				frame.addTorrent(magnet.getTorrent());
			} else if(arg.startsWith("-no-gui")) {
				showGui = false;
			}
		}
		if(showGui) {
			frame.setVisible(true);
		} else {
			frame.dispose();
		}
	}

}
