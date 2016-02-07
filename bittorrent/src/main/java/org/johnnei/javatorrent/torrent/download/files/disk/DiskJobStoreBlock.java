package org.johnnei.javatorrent.torrent.download.files.disk;

import java.io.IOException;

import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskJobStoreBlock extends DiskJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiskJobStoreBlock.class);

	private int pieceIndex;
	private int blockIndex;
	private byte[] data;

	public DiskJobStoreBlock(int pieceIndex, int blockIndex, byte[] data) {
		this.pieceIndex = pieceIndex;
		this.blockIndex = blockIndex;
		this.data = data;
	}

	@Override
	public void process(Torrent torrent) {
		try {
			torrent.getFiles().getPiece(pieceIndex).storeBlock(blockIndex, data);
			if (torrent.getFiles().getPiece(pieceIndex).isDone()) {
				torrent.addToHaltingOperations(1);
				torrent.addDiskJob(new DiskJobCheckHash(pieceIndex));
			}
		} catch (TorrentException e) {
			LOGGER.warn(e.getMessage(), e);
			torrent.getFiles().getPiece(pieceIndex).reset(blockIndex);
		} catch (IOException e) {
			LOGGER.warn(String.format("IO error while saving piece %d block %d: %s. Requeueing write task.", pieceIndex, blockIndex, e.getMessage()), e);
			torrent.addDiskJob(this);
			return;
		}
		torrent.finishHaltingOperations(1);
	}

	@Override
	public int getPriority() {
		return CRITICAL;
	}

}
