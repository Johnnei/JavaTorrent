package torrent;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.utils.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import torrent.download.MagnetLink;
import torrent.download.Torrent;
import torrent.download.tracker.TrackerManager;
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

		TorrentClient torrentClient = TorrentClient.Builder
				.createDefaultBuilder()
				.build();

		// Initialise managers

		TorrentManager torrentManager = new TorrentManager();
		TrackerManager trackerManager = new TrackerManager(torrentClient.getConnectionDegradation());

		Thread trackerManagerThread = new Thread(trackerManager, "Tracker manager");
		trackerManagerThread.setDaemon(true);
		trackerManagerThread.start();

		torrentManager.startListener(trackerManager);

		TorrentFrame frame= new TorrentFrame(torrentManager, trackerManager);
		boolean showGui = true;
		for(int i = 0; i < args.length; i++) {
			String arg = args[i];
			if(arg.startsWith("magnet")) {
				MagnetLink magnet = new MagnetLink(arg, torrentManager, trackerManager);
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
