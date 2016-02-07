package org.johnnei.javatorrent.torrent.download.algos;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.encoding.SHA1;
import org.johnnei.javatorrent.utils.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AMetadataPhase implements IDownloadPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(AMetadataPhase.class);

	protected Torrent torrent;

	/**
	 * If the metadata file was found before this phase started
	 */
	protected boolean foundMatchingFile;

	protected TorrentClient torrentClient;

	public AMetadataPhase(TorrentClient torrentClient, Torrent torrent) {
		this.torrentClient = torrentClient;
		this.torrent = torrent;
	}

	@Override
	public void onPhaseEnter() {
		File file = Config.getConfig().getTorrentFileFor(torrent.getHash());
		if(!file.exists()) {
			return;
		}

		try (RandomAccessFile fileAccess = new RandomAccessFile(file, "r")) {
			byte[] data = new byte[(int)fileAccess.length()];
			fileAccess.seek(0);
			fileAccess.read(data, 0, data.length);
			if(Arrays.equals(SHA1.hash(data), torrent.getHashArray())) {
				foundMatchingFile = true;
				LOGGER.info("Found pre-downloaded Torrent file");
			}
		} catch (IOException e) {
		}
	}

}
