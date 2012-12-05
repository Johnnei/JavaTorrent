package torrent;

import torrent.download.MagnetLink;
import torrent.download.Torrent;
import torrent.frame.TorrentFrame;

public class JavaTorrent extends Thread {

	public static final String BUILD = "JavaTorrent 0.01.0 Dev";

	private TorrentFrame frame;

	public static void main(String[] args) {
		System.out.println("Parsing Magnet Link...");
		if (args.length == 0) {
			System.err.println("Magnet Link not found");
			System.exit(1);
		}
		MagnetLink magnet = new MagnetLink(args[0]);
		if (magnet.isDownloadable()) {
			magnet.getTorrent().start();
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
			// try {
			frame.updateData();
			frame.repaint();
			Torrent.sleep(1000);
			/*
			 * } catch (Exception e) { }
			 */
		}
	}

}
