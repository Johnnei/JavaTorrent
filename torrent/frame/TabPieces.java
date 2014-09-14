package torrent.frame;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Collections;

import torrent.download.Files;
import torrent.download.Torrent;
import torrent.download.files.Piece;
import torrent.frame.controls.TableBase;
import torrent.util.StringUtil;

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
		g.drawString("Blocks (Need | Req)", 200, getHeaderTextY());
		g.drawString("Progress", 325, getHeaderTextY());
	}

	@Override
	protected void paintData(Graphics g) {
		if (torrent == null)
			return;
		if (torrent.getDownloadStatus() != Torrent.STATE_DOWNLOAD_DATA)
			return;
		ArrayList<Piece> pieceList = new ArrayList<>();
		Files tf = torrent.getFiles();
		for (int i = 0; i < tf.getPieceCount(); i++) {
			if (tf.getPiece(i).isStarted() && !tf.getPiece(i).isDone()) {
				pieceList.add(torrent.getFiles().getPiece(i));
			}
		}
		
		Collections.sort(pieceList);;
		setItemCount(pieceList.size());
		for (int i = pieceList.size() - 1; i >= 0; i--) {
			if (isVisible()) {
				Piece p = (Piece) pieceList.get(i);
				g.setColor(Color.BLACK);
				g.drawString("" + p.getIndex(), 5, getTextY());
				g.drawString(StringUtil.compactByteSize(p.getSize()), 100, getTextY());
				g.drawString((p.getBlockCount() - p.getDoneCount()) + " | " + p.getRequestedCount(), 200, getTextY());
				for (int j = 0; j < p.getBlockCount(); j++) {
					if (p.isDone(j)) {
						g.setColor(Color.GREEN);
					} else if (p.isRequested(j)) {
						g.setColor(Color.ORANGE);
					} else {
						g.setColor(Color.RED);
					}
					g.fillRect(326 + j, getDrawY(), 2, 15);
					g.setColor(Color.BLACK);
					g.drawRect(325, getDrawY(), p.getBlockCount() + 2, 15);
				}
			}
			advanceLine();
		}
	}
}
