package torrent;

import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.download.MagnetLink;
import torrent.download.Torrent;
import torrent.frame.TorrentFrame;
import torrent.util.Logger;

public class JavaTorrent extends Thread {

	public static final String BUILD = "JavaTorrent 0.05.0";

	private TorrentFrame frame;

	public static void main(String[] args) {
		System.setOut(new Logger(System.out));
		System.setErr(new Logger(System.err));
		Config.getConfig().load();
		TorrentFrame frame = new TorrentFrame();
		if (args.length > 0) {
			MagnetLink magnet = new MagnetLink(args[0]);
			if (magnet.isDownloadable()) {
				Torrent torrent = magnet.getTorrent();
				torrent.initialise();
				torrent.start();
			} else {
				System.err.println("Magnet link error occured");
			}
			frame.addTorrent(magnet.getTorrent());
		}
		new JavaTorrent(frame).start();
	}

	public JavaTorrent(TorrentFrame frame) {
		this.frame = frame;
	}

	public void run() {
		while (true) {
			long startTime = System.currentTimeMillis();
			frame.updateData();
			frame.repaint();
			System.gc();
			int duration = 1000 - (int) (System.currentTimeMillis() - startTime);
			ThreadUtils.sleep(duration);
		}
	}

}
