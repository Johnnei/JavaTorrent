package org.johnnei.javatorrent.torrent.download.files.disk;

import java.io.IOException;

import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.torrent.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A job to check the hash of a piece for a given torrent
 *
 * @author Johnnei
 *
 */
public class DiskJobCheckHash extends DiskJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiskJobCheckHash.class);

	/**
	 * The piece to check the has for
	 */
	private int pieceIndex;

	public DiskJobCheckHash(int pieceIndex) {
		this.pieceIndex = pieceIndex;
	}

	@Override
	public void process(Torrent torrent) {
		try {
			if (torrent.getFiles().getPiece(pieceIndex).checkHash()) {
				if (!torrent.isDownloadingMetadata()) {
					// Why the heck is this here?!
					torrent.getFiles().havePiece(pieceIndex);
					torrent.broadcastMessage(new MessageHave(pieceIndex));
				}
				LOGGER.info("Recieved and verified piece: " + pieceIndex + ", Torrent Progress: " + StringUtil.progressToString(torrent.getProgress()) + "%");
			} else {
				LOGGER.warn("Hash check failed on piece: " + pieceIndex);
				torrent.getFiles().getPiece(pieceIndex).hashFail();
			}
		} catch (TorrentException e) {
			LOGGER.warn("Hash check error on piece: " + pieceIndex + ", Err: " + e.getMessage());
			torrent.getFiles().getPiece(pieceIndex).hashFail();
		} catch (IOException e) {
			LOGGER.warn(String.format("IO error while checking hash on piece %d. Requeuing task.", pieceIndex), e);
			return;
		}
		torrent.finishHaltingOperations(1);
	}

	@Override
	public int getPriority() {
		return HIGH;
	}

}
