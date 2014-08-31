package torrent;

import org.johnnei.utils.config.Config;

import torrent.download.MagnetLink;
import torrent.download.Torrent;
import torrent.frame.TorrentFrame;
import torrent.util.Logger;

public class JavaTorrent extends Thread {

	public static final String BUILD = "JavaTorrent 0.05.0";

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
		System.setOut(new Logger(System.out));
		System.setErr(new Logger(System.err));
		loadDefaultConfig();
		TorrentFrame frame= new TorrentFrame();
		boolean showGui = true;
		for(int i = 0; i < args.length; i++) {
			String arg = args[i];
			if(arg.startsWith("magnet")) {
				MagnetLink magnet = new MagnetLink(arg);
				if (magnet.isDownloadable()) {
					Torrent torrent = magnet.getTorrent();
					torrent.initialise();
					torrent.start();
				} else {
					System.err.println("Magnet link error occured");
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
