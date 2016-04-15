package org.johnnei.javatorrent.phases;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.choking.PermissiveStrategy;

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

	protected File metadataFile;

	private IChokingStrategy chokingStrategy;

	public AMetadataPhase(TorrentClient torrentClient, Torrent torrent, File metadataFile) {
		this.torrentClient = torrentClient;
		this.torrent = torrent;
		this.metadataFile = metadataFile;
		chokingStrategy = new PermissiveStrategy();
	}

	@Override
	public void onPhaseEnter() {
		if(!metadataFile.exists()) {
			return;
		}

		try (RandomAccessFile fileAccess = new RandomAccessFile(metadataFile, "r")) {
			byte[] data = new byte[(int)fileAccess.length()];
			fileAccess.seek(0);
			fileAccess.read(data, 0, data.length);
			if(Arrays.equals(SHA1.hash(data), torrent.getHashArray())) {
				foundMatchingFile = true;
				LOGGER.info("Found pre-downloaded Torrent file");
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to verify existing metadata file. Assuming invalid.", e);
		}
	}

	@Override
	public IChokingStrategy getChokingStrategy() {
		return chokingStrategy;
	}
}
