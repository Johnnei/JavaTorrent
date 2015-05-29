package torrent.frame.table;

import javax.swing.table.AbstractTableModel;

import torrent.download.Torrent;
import torrent.download.files.Piece;
import torrent.frame.TorrentFrame;

public class PiecesTableModel extends AbstractTableModel {

	private static final long serialVersionUID = 1L;

	private static final int COL_PIECE_NUM = 0;
	private static final int COL_SIZE = 1;
	private static final int COL_BLOCKS = 2;
	private static final int COL_PROGRESS =3;
	
	private static final String[] headers = new String[] {
		"Piece #",
		"Size",
		"Blocks (Need | Req)",
		"Progress"
	};

	private TorrentFrame torrentFrame;
	
	public PiecesTableModel(TorrentFrame torrentFrame) {
		this.torrentFrame = torrentFrame;
	}

	@Override
	public int getRowCount() {
		Torrent torrent = torrentFrame.getSelectedTorrent();
		
		if (torrent == null || torrent.getFiles() == null) {
			return 0;
		}
		
		return torrent.getFiles().getPieceCount();
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
		Piece piece = torrentFrame.getSelectedTorrent().getFiles().getPiece(rowIndex);
		
		switch (columnIndex) {
		case COL_PIECE_NUM:
			return piece.getIndex();
		case COL_BLOCKS:
			return String.format("%d | %d", piece.getBlockCount() - piece.getDoneCount(), piece.getRequestedCount());
		case COL_PROGRESS:
			return piece;
		case COL_SIZE:
			return piece.getSize();
		default:
			throw new IllegalArgumentException(String.format("Column %d is outside of the column range", columnIndex));
		}
	}

}
