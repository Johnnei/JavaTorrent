package torrent.frame;

import java.awt.Graphics;
import java.util.Collection;

import torrent.download.FileInfo;
import torrent.download.Torrent;
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
		Collection<FileInfo> f = torrent.getFiles().getFiles();
		setItemCount(f.size());
		int i = 0;
		for (FileInfo fileInfo : f) {
			if (isVisible()) {
				if (i++ == getSelectedIndex()) {
					drawSelectedBackground(g);
				}
				Graphics name = g.create();
				name.clipRect(0, getDrawY(), getWidth() - 210, 25);
				name.drawString(fileInfo.getFilename(), 5, getTextY());
				g.drawString(StringUtil.compactByteSize(fileInfo.getSize()), getWidth() - 200, getTextY());
				g.drawString(fileInfo.getPieceHaveCount() + "/" + fileInfo.getPieceCount(), getWidth() - 100, getTextY());
			}
			advanceLine();
		}
	}
}
