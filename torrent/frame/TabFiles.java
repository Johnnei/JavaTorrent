package torrent.frame;

import java.awt.Graphics;

import torrent.download.FileInfo;
import torrent.download.Torrent;
import torrent.download.files.Piece;
import torrent.frame.controls.TableBase;
import torrent.util.StringUtil;

public class TabFiles extends TableBase {

	public static final long serialVersionUID = 1L;
	private Torrent torrent;

	public TabFiles() {
		super(25);
	}

	public void setTorrent(Torrent torrent) {
		this.torrent = torrent;
	}

	@Override
	protected void paintHeader(Graphics g) {
		g.drawString("Filename", 5, getHeaderTextY());
		g.drawString("Size", getWidth() - 200, getHeaderTextY());
		g.drawString("Pieces", getWidth() - 100, getHeaderTextY());
	}

	@Override
	protected void paintData(Graphics g) {
		if (torrent == null)
			return;
		if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA)
			return;
		FileInfo[] f = torrent.getFiles().getFiles();
		setItemCount(f.length);
		for (int i = 0; i < f.length; i++) {
			if(isVisible()) {
				if(i == getSelectedIndex()) {
					drawSelectedBackground(g);
				}
				Graphics name = g.create();
				name.clipRect(0, getDrawY(), getWidth() - 210, 25);
				name.drawString(f[i].getFilename(), 5, getTextY());
				g.drawString(StringUtil.compactByteSize(f[i].getSize()), getWidth() - 200, getTextY());
				int pieceCount = (int) Math.ceil(f[i].getSize() / (double) torrent.getFiles().getPieceSize());
				g.drawString(f[i].getPieceHaveCount() + "/" + pieceCount, getWidth() - 100, getTextY());
			}
			advanceLine();
		}
	}
}
