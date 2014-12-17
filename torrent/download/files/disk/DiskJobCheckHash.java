package torrent.download.files.disk;

import java.io.IOException;

import torrent.TorrentException;
import torrent.download.Torrent;
import torrent.util.StringUtil;

/**
 * A job to check the hash of a piece for a given torrent
 * 
 * @author Johnnei
 * 
 */
public class DiskJobCheckHash extends DiskJob {

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
				if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_DATA) {
					torrent.broadcastHave(pieceIndex);
				}
				torrent.getLogger().info("Recieved and verified piece: " + pieceIndex + ", Torrent Progress: " + StringUtil.progressToString(torrent.getProgress()) + "%");
			} else {
				torrent.getLogger().warning("Hash check failed on piece: " + pieceIndex);
				torrent.getFiles().getPiece(pieceIndex).hashFail();
			}
		} catch (TorrentException e) {
			torrent.getLogger().warning("Hash check error on piece: " + pieceIndex + ", Err: " + e.getMessage());
			torrent.getFiles().getPiece(pieceIndex).hashFail();
		} catch (IOException e) {
			torrent.getLogger().warning(String.format("IO error while checking hash on piece %d: %s. Requeuing task.", pieceIndex, e.getMessage()));
			return;
		}
		torrent.addToHaltingOperations(-1);
	}

	@Override
	public int getPriority() {
		return HIGH;
	}

}
