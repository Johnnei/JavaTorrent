package torrent.frame;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;

import javax.swing.JPanel;

import torrent.download.Torrent;
import torrent.util.StringUtil;

public class TorrentList extends JPanel implements MouseListener {

	private static final long serialVersionUID = 1L;
	private ArrayList<Torrent> torrents;
	private int selectedIndex;
	
	public TorrentList() {
		torrents = new ArrayList<Torrent>();
		selectedIndex = 0;
	}
	
	public void add(Torrent torrent) {
		torrents.add(torrent);
	}
	
	public Color getForegroundColor() {
		return Color.BLACK;
	}
	
	public Color getBackgroundColor() {
		return Color.WHITE;
	}
	
	public Color getSelectedBackgroundColor() {
		return new Color(0x00, 0xC0, 0xFF);
	}
	
	public void paintComponent(Graphics g) {
		g.setColor(getBackgroundColor());
		g.fillRect(0, 0, getWidth(), getHeight());
		//Header
		g.setColor(new Color(0xC8, 0xDD, 0xF2));
		g.fillRect(0, 0, getWidth(), 25);
		g.setColor(getForegroundColor());
		g.drawString("Name", 5, 16);
		g.drawString("Progress", 400, 16);
		g.drawString("Download Speed", 610, 16);
		g.drawString("Upload Speed", 725, 16);
		g.drawString("Seeders", 825, 16);
		g.drawString("Leechers", 890, 16);
		//Torrents
		for(int i = 0; i < torrents.size(); i++) {
			Torrent torrent = torrents.get(i);
			int y = (i + 1) * 25;
			//Background
			if(selectedIndex == i) {
				g.setColor(getSelectedBackgroundColor());
				g.fillRect(0, y, getWidth(), 25);
			}
			//Display Name
			g.setColor(getForegroundColor());
			g.drawString(torrent.getDisplayName(), 5, 16 + y);
			//Progress Bar
			double p = (int)torrent.getProgress() * 2;
			g.drawRect(400, y, 201, 24);
			g.setColor(Color.RED);
			g.fillRect(401 + (int)p, y + 1, 200 - (int)p, 23);
			if(torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA)
				g.setColor(Color.ORANGE);
			else
				g.setColor(Color.GREEN);
			g.fillRect(401, y + 1, (int)p, 23);
			g.setColor(getForegroundColor());
			drawCenteredString(g, (p / 2) + "%", 500, y + 16);
			//Speeds
			drawStringRightToLeft(g, StringUtil.compactByteSize(torrent.getDownloadRate()) + "/s", 700, y + 16);
			drawStringRightToLeft(g, StringUtil.compactByteSize(torrent.getUploadRate()) + "/s", 800, y + 16);
			//Leechers and Seeders
			g.drawString("" + torrent.getSeedCount(), 825, y + 16);
			g.drawString("" + torrent.getLeecherCount(), 890, y + 16);
		}
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
	public void mouseClicked(MouseEvent e) {
		
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		
	}

	public void setSelectedIndex(int index) {
		selectedIndex = index;
	}

}
