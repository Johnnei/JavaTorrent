package torrent.download.files.disk;

import torrent.TorrentException;
import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.protocol.messages.MessageBlock;

public class DiskJobSendBlock extends DiskJob {

	/**
	 * The peer to send the block to
	 */
	private Peer peer;
	private int pieceIndex;
	private int offset;
	private int length;

	public DiskJobSendBlock(Peer peer, int pieceIndex, int offset, int length) {
		this.pieceIndex = pieceIndex;
		this.offset = offset;
		this.length = length;
		this.peer = peer;
	}

	@Override
	public void process(Torrent torrent) {
		byte[] data = new byte[0];
		try {
			data = torrent.getFiles().getPiece(pieceIndex).loadPiece(offset, length);
		} catch (TorrentException te) {
			te.printStackTrace();
		}
		peer.addToQueue(new MessageBlock(pieceIndex, offset, data));
		peer.addToPendingMessages(-1);
	}

	@Override
	public int getPriority() {
		return NORMAL;
	}

}
