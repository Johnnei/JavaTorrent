package torrent.frame;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import torrent.download.Torrent;
import torrent.frame.controls.TableBase;
import torrent.util.StringUtil;

public class TorrentList extends TableBase {

	private static final long serialVersionUID = 1L;
	private ArrayList<Torrent> torrents;
	
	public TorrentList() {
		super(25);
		torrents = new ArrayList<Torrent>();
	}
	
	public void add(Torrent torrent) {
		torrents.add(torrent);
	}
	
	private void drawCenteredString(Graphics g, String s, int x, int y) {
		int i = (int)g.getFontMetrics().getStringBounds(s, g).getWidth() / 2;
		g.drawString(s, x - i, y);
	}
	
	private void drawStringRightToLeft(Graphics g, String s, int x, int y) {
		int i = (int)g.getFontMetrics().getStringBounds(s, g).getWidth();
		g.drawString(s, x - i, y);
	}

	@Override
	protected void paintHeader(Graphics g) {
		g.drawString("Name", 5, getHeaderTextY() + 3);
		g.drawString("Progress", 400, getHeaderTextY() + 3);
		g.drawString("Download Speed", 610, getHeaderTextY() + 3);
		g.drawString("Upload Speed", 725, getHeaderTextY() + 3);
		g.drawString("Seeders", 825, getHeaderTextY() + 3);
		g.drawString("Leechers", 890, getHeaderTextY() + 3);
	}

	@Override
	protected void paintData(Graphics g) {
		for(int i = 0; i < torrents.size(); i++) {
			Torrent torrent = torrents.get(i);
			//Background
			if(getSelectedIndex() == i) {
				drawSelectedBackground(g);
			}
			g.drawString(torrent.getDisplayName(), 5, getTextY());
			//Progress Bar
			double p = torrent.getProgress() * 2;
			g.drawRect(400, getDrawY() + 2, 201, 19);
			g.setColor(Color.RED);
			g.fillRect(401 + (int)p, getDrawY() + 3, 200 - (int)p, 18);
			if(torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA)
				g.setColor(Color.ORANGE);
			else
				g.setColor(Color.GREEN);
			g.fillRect(401, getDrawY() + 3, (int)p, 18);
			g.setColor(getForegroundColor());
			drawCenteredString(g, StringUtil.progressToString(p / 2) + "%", 500, getDrawY() + 16);
			//Speeds
			drawStringRightToLeft(g, StringUtil.compactByteSize(torrent.getDownloadRate()) + "/s", 700, getTextY());
			drawStringRightToLeft(g, StringUtil.compactByteSize(torrent.getUploadRate()) + "/s", 800, getTextY());
			//Leechers and Seeders
			g.drawString("" + torrent.getSeedCount(), 825, getTextY());
			g.drawString("" + torrent.getLeecherCount(), 890, getTextY());
			advanceLine();
		}
	}

}
