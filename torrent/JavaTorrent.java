package torrent;

import org.johnnei.utils.ThreadUtils;

import torrent.download.MagnetLink;
import torrent.download.Torrent;
import torrent.frame.TorrentFrame;

public class JavaTorrent extends Thread {

	public static final String BUILD = "JavaTorrent 0.04.0";

	private TorrentFrame frame;

	public static void main(String[] args) {
		System.out.println("Parsing Magnet Link...");
		if (args.length == 0) {
			System.err.println("Magnet Link not found");
			System.exit(1);
		}
		MagnetLink magnet = new MagnetLink(args[0]);
		if (magnet.isDownloadable()) {
			Torrent torrent = magnet.getTorrent();
			torrent.initialise();
			torrent.start();
		} else {
			System.err.println("Magnet link error occured");
		}
		TorrentFrame frame = new TorrentFrame();
		frame.addTorrent(magnet.getTorrent());
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
			int duration = 1000 - (int)(System.currentTimeMillis() - startTime);
			ThreadUtils.sleep(duration);
		}
	}

}
