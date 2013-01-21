package torrent.frame;

import java.awt.Graphics;

import torrent.download.Torrent;
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
		if(torrent != null) {
			Tracker[] trackers = torrent.getTrackers();
			int count = 0;
			for(int i = 0; i < trackers.length; i++) {
				if(trackers[i] != null) {
					++count;
					if(isVisible()) {
						g.drawString(trackers[i].getTrackerName(), 5, getTextY());
						g.drawString(trackers[i].getStatus(), 200, getTextY());
						g.drawString(trackers[i].getSeeders() + " (" + trackers[i].getSeedersInSwarm() + ")", 350, getTextY());
						g.drawString(trackers[i].getLeechers() + " (" + trackers[i].getLeechersInSwarm() + ")", 450, getTextY());
						g.drawString(trackers[i].getDownloadedCount() + "", 550, getTextY());
					}
				}
				advanceLine();
			}
			setItemCount(count);
		}
	}
}
