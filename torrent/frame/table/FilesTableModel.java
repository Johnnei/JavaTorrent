package torrent.frame.table;

import javax.swing.table.AbstractTableModel;

import torrent.download.FileInfo;
import torrent.download.Torrent;
import torrent.frame.TorrentFrame;
import torrent.util.StringUtil;

public class FilesTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;
	private static final int COL_NAME = 0;
	private static final int COL_SIZE = 1;
	private static final int COL_PIECES = 2;
	
	private static final String[] headers = {
		"Filename",
		"Pieces",
		"Size"
	};
	
	private TorrentFrame torrentFrame;

	public FilesTableModel(TorrentFrame torrentFrame) {
		this.torrentFrame = torrentFrame;
	}

	@Override
	public int getRowCount() {
		Torrent torrent = torrentFrame.getSelectedTorrent();
		
		if (torrent == null || torrent.getFiles() == null) {
			return 0;
		}
		
		return torrent.getFiles().getFiles().size();
	}

	@Override
	public int getColumnCount() {
		return headers.length;
	}
	
	@Override
	public String getColumnName(int column) {
		return headers[column];
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Torrent torrent = torrentFrame.getSelectedTorrent();
		FileInfo file = torrent.getFiles().getFiles().get(rowIndex);
		
		switch (columnIndex) {
			case COL_NAME:
				return file.getFilename();
			case COL_PIECES:
				return String.format("%d/%d", file.getPieceHaveCount(), file.getPieceCount());
			case COL_SIZE:
				return StringUtil.compactByteSize(file.getSize());
			default:
				throw new IllegalArgumentException(String.format("Column %d is outside of the column range", columnIndex));
		}
	}

}
