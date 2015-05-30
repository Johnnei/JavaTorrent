package torrent.frame.table;

import java.util.List;

import torrent.download.Torrent;
import torrent.util.StringUtil;

public class TorrentTableModel extends GenericTableModel<Torrent> {
	
	private static final long serialVersionUID = 1L;
	
	private static final int COL_NAME = 0;
	private static final int COL_PROGRESS = 1;
	private static final int COL_DOWNLOAD_SPEED = 2;
	private static final int COL_UPLOAD_SPEED = 3;
	private static final int COL_SEEDERS = 4;
	private static final int COL_LEECHERS = 5;

	public TorrentTableModel(List<Torrent> torrents) {
		super(torrents, new String[] { "Name", "Progress", "Download speed", "Upload speed", "Seeders", "Leechers" });
	}

	@Override
	public Object getValueForColumn(Torrent torrent, int column) {
		switch (column) {
			case COL_NAME:
				return torrent.getDisplayName();
			case COL_PROGRESS:
				return torrent.getProgress();
			case COL_DOWNLOAD_SPEED:
				return String.format("%s/s", StringUtil.compactByteSize(torrent.getDownloadRate()));
			case COL_UPLOAD_SPEED:
				return String.format("%s/s", StringUtil.compactByteSize(torrent.getUploadRate()));
			case COL_SEEDERS:
				return torrent.getSeedCount();
			case COL_LEECHERS:
				return torrent.getLeecherCount();
			default:
				throw new IllegalArgumentException(String.format("Column %d is outside of the column range", column));
		}
	}
	
	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == COL_PROGRESS) {
			return Double.class;
		}
		
		return super.getColumnClass(columnIndex);
	}

}
