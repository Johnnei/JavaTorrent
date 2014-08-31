package torrent.frame;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import torrent.Manager;
import torrent.download.Torrent;
import torrent.util.StringUtil;

public class TabGeneral extends JPanel {

	public static final long serialVersionUID = 1L;

	private Torrent torrent;

	public void setTorrent(Torrent torrent) {
		this.torrent = torrent;
	}

	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		String name = "Name: ";
		String hash = "Hash: ";
		String totalSize = "Total Size: ";
		String leftSize = "Remaining: ";
		String pieces = "Pieces: ";
		String uploaded = "Uploaded: ";
		String peers = "Peers: ";
		String leechers = "Leechers: ";
		String pending = "Pending Connections: ";
		String download = "Download Speed: ";
		String upload = "Upload Speed: ";

		if (torrent != null) {
			name += torrent.getDisplayName();
			hash += torrent.getHash();
			if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA) {
				totalSize += "Retrieving metadata";
				pieces += "Retrieving metadata";
				leftSize += "Retrieving metadata";
				uploaded += "Retrieving metadata";
			} else {
				totalSize += StringUtil.compactByteSize(torrent.getFiles().getTotalSize());
				pieces += torrent.getFiles().getBitfield().hasPieceCount() + "/" + torrent.getFiles().getPieceCount();
				leftSize += StringUtil.compactByteSize(torrent.getFiles().getRemainingBytes());
				uploaded += StringUtil.compactByteSize(torrent.getUploadedBytes());
			}
			pending += Integer.toString(Manager.getTrackerManager().getConnectingCountFor(torrent));
			peers += torrent.getSeedCount();
			leechers += torrent.getLeecherCount();
			download += StringUtil.compactByteSize(torrent.getDownloadRate()) + "/s";
			upload += StringUtil.compactByteSize(torrent.getUploadRate()) + "/s";
		}

		double progress = 0;
		if (torrent != null) {
			progress = torrent.getProgress();
		}
		g.setColor(Color.GRAY);
		g.drawRect(10, 10, getWidth() - 20, 10);
		if (torrent != null) {
			if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA)
				g.setColor(Color.ORANGE);
			else
				g.setColor(Color.GREEN);
		} else
			g.setColor(Color.GREEN);
		double pixelsPerPercentage = (getWidth() - 21) * 0.01;
		g.fillRect(11, 11, (int) (progress * pixelsPerPercentage), 9);

		g.setColor(Color.BLACK);
		g.drawString(name, 10, 50);
		g.drawString(hash, 10, 70);
		g.drawString(totalSize, 10, 90);
		g.drawString(leftSize, 10, 110);
		g.drawString(pieces, 10, 130);
		g.drawString(uploaded, 10, 150);

		int x = getWidth() / 2;
		g.drawString(peers, x, 50);
		g.drawString(leechers, x, 70);
		g.drawString(pending, x, 90);
		g.drawString(download, x, 110);
		g.drawString(upload, x, 130);
	}

}
