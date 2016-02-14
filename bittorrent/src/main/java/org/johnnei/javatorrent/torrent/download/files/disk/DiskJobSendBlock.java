package org.johnnei.javatorrent.torrent.download.files.disk;

import java.io.IOException;

import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.protocol.messages.MessageBlock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskJobSendBlock extends DiskJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiskJobSendBlock.class);

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
		try {
			byte[] data = torrent.getFiles().getPiece(pieceIndex).loadPiece(offset, length);
			peer.getBitTorrentSocket().enqueueMessage(new MessageBlock(pieceIndex, offset, data));
			peer.addToPendingMessages(-1);
			torrent.addUploadedBytes(data.length);
		} catch (TorrentException te) {
			LOGGER.warn(String.format("Can't satisfy peer request for block: %s", te.getMessage()));
		} catch (IOException e) {
			LOGGER.warn(String.format("IO error while reading block request: %s. Requeueing task.", e.getMessage()));
			torrent.addDiskJob(this);
		}
	}

	@Override
	public int getPriority() {
		return NORMAL;
	}

}
