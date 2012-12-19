package torrent.frame;

import java.awt.Graphics;

import torrent.download.Torrent;
import torrent.download.tracker.Tracker;

public class TabTracker extends TableBase {

	public static final long serialVersionUID = 1L;
	private Torrent torrent;

	public TabTracker() {
		super(25);	
	}

	public void setTorrent(Torrent torrent) {
		this.torrent = torrent;
	}

	/*public void updateData() {
		if (torrent == null)
			return;
		Tracker[] trackers = torrent.getTrackers();
		int valid = 0;
		for (int i = 0; i < trackers.length; i++) {
			if (trackers[i] != null)
				valid++;
		}
		Object[][] data = new Object[valid][HEADER.length];
		for (int i = 0; i < trackers.length; i++) {
			if (trackers[i] == null)
				continue;
			data[i][0] = trackers[i].getTrackerName();
			data[i][1] = trackers[i].getStatus();
			data[i][2] = trackers[i].getSeeders();
			data[i][3] = trackers[i].getLeechers();
		}
		final Object[][] invokeData = data;
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				tableModel.setDataVector(invokeData, HEADER);
			}
		});
	}*/

	@Override
	protected void paintHeader(Graphics g) {
		g.drawString("Tracker", 5, getHeaderTextY());
		g.drawString("Status", 200, getHeaderTextY());
		g.drawString("Seeders", 350, getHeaderTextY());
		g.drawString("Leechers", 450, getHeaderTextY());
	}

	@Override
	protected void paintData(Graphics g) {
		if(torrent != null) {
			Tracker[] trackers = torrent.getTrackers();
			for(int i = 0; i < trackers.length; i++) {
				if(isVisible()) {
					if(trackers[i] != null) {
						g.drawString(trackers[i].getTrackerName(), 5, getTextY());
						g.drawString(trackers[i].getStatus(), 200, getTextY());
						g.drawString("" + trackers[i].getSeeders(), 350, getTextY());
						g.drawString("" + trackers[i].getLeechers(), 450, getTextY());
					}
				}
				advanceLine();
			}
		}
	}
}
