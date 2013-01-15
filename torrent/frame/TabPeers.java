package torrent.frame;

import java.awt.Graphics;
import java.util.ArrayList;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.util.ISortable;
import torrent.util.Mergesort;
import torrent.util.StringUtil;

public class TabPeers extends TableBase {

	public static final long serialVersionUID = 1L;
	private Torrent torrent;

	public TabPeers() {
		super(20);
	}

	public void setTorrent(Torrent torrent) {
		this.torrent = torrent;
	}

	protected void paintHeader(Graphics g) {
		g.drawString("IP", 5, getHeaderTextY());
		g.drawString("Client", 160, getHeaderTextY());
		g.drawString("Down Speed", 290, getHeaderTextY());
		g.drawString("Up Speed", 370, getHeaderTextY());
		g.drawString("Time idle", 440, getHeaderTextY());
		g.drawString("Pieces", 510, getHeaderTextY());
		g.drawString("Requests", 570, getHeaderTextY());
		g.drawString("State", 640, getHeaderTextY());
	}
	
	protected void paintData(Graphics g) {
		if (torrent != null) {
			//Sort
			ArrayList<Peer> peers = torrent.getPeers();
			ArrayList<ISortable> toSort = new ArrayList<>();
			for (int i = 0; i < peers.size(); i++) {
				Peer p = peers.get(i);
				if(p.getPassedHandshake())
					toSort.add(p);
			}
			Mergesort peerList = new Mergesort(toSort);
			peerList.sort();
			//Draw
			for (int i = toSort.size() - 1; i >= 0 ; i--) {
				if (rowIsVisible()) {
					Peer peer = (Peer) peerList.getItem(i);
					long duration = (System.currentTimeMillis() - peer.getLastActivity()) / 1000;
					g.drawString(peer.toString(), 5, getTextY());
					g.drawString(peer.getClientName(), 160, getTextY());
					g.drawString(StringUtil.speedToString(peer.getDownloadRate()), 290, getTextY());
					g.drawString(StringUtil.speedToString(peer.getUploadRate()), 370, getTextY());
					g.drawString(duration + " s", 440, getTextY());
					g.drawString("" + peer.getClient().hasPieceCount(), 510, getTextY());
					g.drawString(peer.getWorkQueueSize() + "/" + peer.getMaxWorkLoad() + " | 0", 570, getTextY());
					g.drawString(peer.getStatus(), 640, getTextY());
				}
				advanceLine();
			}
		}
	}
}
