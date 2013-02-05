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
			new JavaTorrent(frame).start();
		} else {
			frame.dispose();
		}
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
