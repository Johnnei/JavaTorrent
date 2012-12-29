package torrent.frame;

import java.awt.Graphics;
import java.util.ArrayList;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.util.Heap;
import torrent.util.HeapSort;

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
		g.drawString("Download Speed", 160, getHeaderTextY());
		g.drawString("Upload Speed", 270, getHeaderTextY());
		g.drawString("Time idle", 370, getHeaderTextY());
		g.drawString("Having Pieces", 440, getHeaderTextY());
		g.drawString("Requests", 540, getHeaderTextY());
		g.drawString("C | I", 610, getHeaderTextY());
		g.drawString("State", 700, getHeaderTextY());
	}
	
	protected void paintData(Graphics g) {
		if (torrent != null) {
			//Sort
			ArrayList<Peer> peers = torrent.getPeers();
			Heap peerHeap = new Heap(peers.size());
			for (int i = 0; i < peers.size(); i++) {
				Peer p = peers.get(i);
				if(p.getPassedHandshake())
					peerHeap.add(p);
			}
			HeapSort peerList = new HeapSort(peerHeap);
			peerList.sort();
			//Draw
			for (int i = 0; i < peerList.getItems().length; i++) {
				if (rowIsVisible()) {
					Peer peer = (Peer) peerList.getItems()[i];
					long duration = (System.currentTimeMillis() - peer.getLastActivity()) / 1000;
					g.drawString(peer.toString(), 5, getTextY());
					g.drawString(peer.getDownloadRate() + " b/s", 160, getTextY());
					g.drawString(peer.getUploadRate() + " b/s", 270, getTextY());
					g.drawString(duration + " s", 370, getTextY());
					g.drawString("" + peer.getClient().hasPieceCount(), 440, getTextY());
					g.drawString(peer.getWorkQueueSize() + " | 0", 540, getTextY());
					g.drawString(peer.getMyClient().isChoked() + " | " + peer.getClient().isInterested(), 610, getTextY());
					g.drawString(peer.getStatus(), 700, getTextY());
				}
				advanceLine();
			}
		}
	}
}
