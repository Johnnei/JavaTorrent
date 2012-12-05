package torrent.frame;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import torrent.download.Torrent;
import torrent.download.TorrentFiles;
import torrent.download.files.Piece;

public class TabPieces extends JPanel {

	public static final long serialVersionUID = 1L;
	private Torrent torrent;

	public TabPieces() {
	}

	public void setTorrent(Torrent torrent) {
		this.torrent = torrent;
	}
	
	public Color getForegroundColor() {
		return Color.BLACK;
	}
	
	public Color getBackgroundColor() {
		return Color.WHITE;
	}
	
	public void paintComponent(Graphics g) {
		int textY = 13;
		
		g.setColor(getBackgroundColor());
		g.fillRect(0, 0, getWidth(), getHeight());
		//Header
		g.setColor(new Color(0xC8, 0xDD, 0xF2));
		g.fillRect(0, 0, getWidth(), 20);
		g.setColor(getForegroundColor());
		g.drawString("Piece #", 5, textY);
		g.drawString("Size", 100, textY);
		g.drawString("Progress", 200, textY);
		g.drawString("Subpieces (Remaining | Requested)", 300, textY);
		if(torrent == null)
			return;
		if(torrent.getDownloadStatus() != Torrent.STATE_DOWNLOAD_DATA)
			return;
		TorrentFiles tf = torrent.getTorrentFiles();
		for(int i = 0; i < tf.getPieceCount(); i++) {
			if(tf.getPiece(i).isStarted()) {
				textY += 20;
				Piece p = torrent.getTorrentFiles().getPieceReadOnly(i);
				g.drawString("" + i, 5, textY);
				g.drawString("" + p.getSize(), 100, textY);
				g.drawString(p.getProgress() + "%", 200, textY);
				g.drawString((p.getSubpieceCount() - p.getDoneCount()) + " | " + p.getRequestedCount(), 300, textY);
			}
		}
	}
}
