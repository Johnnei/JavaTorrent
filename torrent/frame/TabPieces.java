package torrent.frame;

import java.awt.Color;
import java.awt.Graphics;

import torrent.download.Torrent;
import torrent.download.TorrentFiles;
import torrent.download.files.Piece;
import torrent.util.Heap;
import torrent.util.HeapSort;

public class TabPieces extends TableBase {

	public static final long serialVersionUID = 1L;
	private Torrent torrent;

	public TabPieces() {
		super(20);
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
	
	protected void paintHeader(Graphics g) {
		g.drawString("Piece #", 5, getHeaderTextY());
		g.drawString("Size", 100, getHeaderTextY());
		g.drawString("Progress", 200, getHeaderTextY());
		g.drawString("Subpieces (Remaining | Requested)", 300, getHeaderTextY());
	}

	@Override
	protected void paintData(Graphics g) {
		if(torrent == null)
			return;
		if(torrent.getDownloadStatus() != Torrent.STATE_DOWNLOAD_DATA)
			return;
		Heap pieceHeep = new Heap(100);
		TorrentFiles tf = torrent.getTorrentFiles();
		for(int i = 0; i < tf.getPieceCount(); i++) {
			if(tf.getPiece(i).isStarted()) {
				 pieceHeep.add(torrent.getTorrentFiles().getPieceReadOnly(i));
			}
		}
		HeapSort sortedHeap = new HeapSort(pieceHeep);
		sortedHeap.sort();
		for(int i = 0; i < sortedHeap.getItems().length; i++) {
			if(isVisible()) {
				Piece p = (Piece)sortedHeap.getItems()[i];
				g.drawString("" + p.getIndex(), 5, getTextY());
				g.drawString("" + p.getSize(), 100, getTextY());
				g.drawString(p.getProgress() + "%", 200, getTextY());
				g.drawString((p.getBlockCount() - p.getDoneCount()) + " | " + p.getRequestedCount(), 300, getTextY());
			}
			advanceLine();
		}
	}
}
