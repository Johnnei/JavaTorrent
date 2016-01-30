package org.johnnei.javatorrent.torrent.frame.table;

import javax.swing.table.AbstractTableModel;

import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.peer.PeerDirection;
import org.johnnei.javatorrent.torrent.frame.TorrentFrame;
import org.johnnei.javatorrent.torrent.util.StringUtil;

public class PeerTableModel extends AbstractTableModel {
	
	private static final long serialVersionUID = 1L;
	
	private static final String[] headers = new String[] {
		"IP",
		"Client",
		"Down Speed",
		"Up Speed",
		"Time idle",
		"Pieces",
		"Requests",
		"Flags",
		"State"
	};

	private static final int COL_IP = 0;
	private static final int COL_CLIENT = 1;
	private static final int COL_DOWN = 2;
	private static final int COL_UP = 3;
	private static final int COL_IDLE = 4;
	private static final int COL_PIECES = 5;
	private static final int COL_REQUESTS = 6;
	private static final int COL_FLAGS = 7;
	private static final int COL_STATE = 8;
	
	private TorrentFrame torrentFrame;

	public PeerTableModel(TorrentFrame torrentFrame) {
		this.torrentFrame = torrentFrame;
	}

	@Override
	public int getRowCount() {
		Torrent torrent = torrentFrame.getSelectedTorrent();
		
		if (torrent == null) {
			return 0;
		}
		
		return torrent.getPeers().size();
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
		Peer peer = torrent.getPeers().get(rowIndex);
		long duration = (System.currentTimeMillis() - peer.getLastActivity()) / 1000;
		
		switch(columnIndex) {
			case COL_IP:
				return peer.toString();
			case COL_CLIENT:
				return peer.getClientName();
			case COL_DOWN:
				String download = StringUtil.compactByteSize(peer.getBitTorrentSocket().getDownloadRate());
				return String.format("%s/s", download);
			case COL_UP:
				String upload = StringUtil.compactByteSize(peer.getBitTorrentSocket().getUploadRate());
				return String.format("%s/s", upload);
			case COL_IDLE:
				return StringUtil.timeToString(duration);
			case COL_PIECES:
				return peer.countHavePieces();
			case COL_REQUESTS:
				return String.format("%d/%d | %d", peer.getWorkQueueSize(PeerDirection.Download), peer.getRequestLimit(), peer.getWorkQueueSize(PeerDirection.Upload));
			case COL_FLAGS:
				return peer.getFlags();
			case COL_STATE:
				return peer.getStatus();
			default:
				throw new IllegalArgumentException(String.format("Column %d is outside of the column range", columnIndex));
		}
	}

}
