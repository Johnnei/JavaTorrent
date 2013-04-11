package torrent.frame;

import java.awt.Graphics;
import java.util.ArrayList;

import torrent.download.Torrent;
import torrent.download.tracker.TorrentInfo;
import torrent.download.tracker.Tracker;
import torrent.frame.controls.TableBase;

public class TabTracker extends TableBase {

	public static final long serialVersionUID = 1L;
	private Torrent torrent;

	public TabTracker() {
		super(25);
	}

	public void setTorrent(Torrent torrent) {
		this.torrent = torrent;
	}

	@Override
	protected void paintHeader(Graphics g) {
		g.drawString("Tracker", 5, getHeaderTextY());
		g.drawString("Status", 200, getHeaderTextY());
		g.drawString("Seeders", 350, getHeaderTextY());
		g.drawString("Leechers", 450, getHeaderTextY());
		g.drawString("Times Completed", 550, getHeaderTextY());
	}

	@Override
	protected void paintData(Graphics g) {
		if (torrent != null) {
			ArrayList<Tracker> trackers = torrent.getTrackers();
			int count = 0;
			for (Tracker tracker : trackers) {
				++count;
				TorrentInfo torrentInfo = tracker.getInfo(torrent);
				if (isVisible()) {
					if ((count - 1) == getSelectedIndex()) {
						drawSelectedBackground(g);
					}
					g.drawString(tracker.getName(), 5, getTextY());
					g.drawString(tracker.getStatus(), 200, getTextY());
					g.drawString("" + torrentInfo.getSeeders(), 350, getTextY());
					g.drawString("" + torrentInfo.getLeechers(), 450, getTextY());
					g.drawString(torrentInfo.getDownloadCount(), 550, getTextY());
				}
				advanceLine();
			}
			setItemCount(count);
		}
	}
}
