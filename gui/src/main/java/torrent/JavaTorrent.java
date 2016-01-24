package torrent;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.johnnei.utils.ConsoleLogger;
import org.johnnei.utils.config.Config;

import torrent.download.MagnetLink;
import torrent.download.Torrent;
import torrent.download.tracker.TrackerManager;
import torrent.frame.TorrentFrame;

public class JavaTorrent extends Thread {

	private static Logger log;

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
		log = ConsoleLogger.createLogger("JavaTorrent", Level.INFO);
		loadDefaultConfig();

		// Initialise managers

		TorrentManager torrentManager = new TorrentManager();
		TrackerManager trackerManager = new TrackerManager();

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
					log.severe("Magnet link error occured");
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
