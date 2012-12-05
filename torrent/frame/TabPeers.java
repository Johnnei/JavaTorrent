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
		super();
	}

	public void setTorrent(Torrent torrent) {
		this.torrent = torrent;
	}

	protected void paintHeader(Graphics g) {
		g.drawString("IP", 5, 13);
		g.drawString("Download Speed", 160, 13);
		g.drawString("Upload Speed", 270, 13);
		g.drawString("Time idle", 370, 13);
		g.drawString("Having Pieces", 440, 13);
		g.drawString("Requests (Down | maxDown | up)", 540, 13);
	}
	
	protected void paintData(Graphics g) {
		if (torrent != null) {
			int textY = 13;
			//Sort
			ArrayList<Peer> peers = torrent.getPeers();
			Heap peerHeap = new Heap(peers.size());
			for (int i = 0; i < peers.size(); i++) {
				peerHeap.add(peers.get(i));
			}
			HeapSort peerList = new HeapSort(peerHeap);
			peerList.sort();
			//Draw
			for (int i = 0; i < peerList.getItems().length; i++) {
				textY += 20;
				if (textY >= getScrollY() && textY < (getScrollY() + 400)) {
					Peer peer = (Peer) peerList.getItems()[i];
					long duration = (System.currentTimeMillis() - peer.getLastActivity()) / 1000;
					g.drawString(peer.toString(), 5, textY - getScrollY());
					g.drawString(peer.getDownloadRate() + " b/s", 160, textY - getScrollY());
					g.drawString(peer.getUploadRate() + " b/s", 270, textY - getScrollY());
					g.drawString(duration + " s", 370, textY - getScrollY());
					g.drawString("" + peer.getClient().hasPieceCount(), 440, textY - getScrollY());
					g.drawString(peer.getWorkQueue() + " | " + peer.getMaxWorkLoad() + " | 0", 540, textY - getScrollY());
				}
			}
		}
	}
}
