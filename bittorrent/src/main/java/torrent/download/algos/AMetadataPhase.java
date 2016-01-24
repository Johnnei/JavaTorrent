package torrent.download.algos;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import org.johnnei.utils.config.Config;

import torrent.download.Torrent;
import torrent.download.tracker.TrackerManager;
import torrent.encoding.SHA1;

public abstract class AMetadataPhase implements IDownloadPhase {

	protected Torrent torrent;

	/**
	 * If the metadata file was found before this phase started
	 */
	protected boolean foundMatchingFile;

	protected TrackerManager trackerManager;

	public AMetadataPhase(TrackerManager trackerManager, Torrent torrent) {
		this.trackerManager = trackerManager;
		this.torrent = torrent;
	}

	@Override
	public void preprocess() {
		File file = Config.getConfig().getTorrentFileFor(torrent.getHash());
		if(!file.exists()) {
			return;
		}

		try (RandomAccessFile fileAccess = new RandomAccessFile(file, "r")) {
			byte[] data = new byte[(int)fileAccess.length()];
			fileAccess.seek(0);
			fileAccess.read(data, 0, data.length);
			if(SHA1.match(SHA1.hash(data), torrent.getHashArray())) {
				foundMatchingFile = true;
				torrent.getLogger().info("Found pre-downloaded Torrent file");
			}
		} catch (IOException e) {
		}
	}

	@Override
	public byte getId() {
		return Torrent.STATE_DOWNLOAD_METADATA;
	}

}
