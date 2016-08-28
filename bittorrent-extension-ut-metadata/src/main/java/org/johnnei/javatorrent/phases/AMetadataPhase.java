package org.johnnei.javatorrent.phases;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.module.UTMetadataExtension;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.choking.PermissiveStrategy;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;

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

	protected final File metadataFile;

	protected final File downloadFolderRoot;

	private IChokingStrategy chokingStrategy;

	public AMetadataPhase(TorrentClient torrentClient, Torrent torrent) {
		this.torrentClient = torrentClient;
		this.torrent = torrent;

		ExtensionModule extensionModule = torrentClient.getModule(ExtensionModule.class)
				.orElseThrow(() -> new IllegalStateException("Metadata phase registered without registering Extension module."));
		UTMetadataExtension metadataExtension = (UTMetadataExtension) extensionModule.getExtensionByName(UTMetadata.NAME)
				.orElseThrow(() -> new IllegalStateException("Metadata phase registered without registering ut_metadata extension."));

		this.metadataFile = metadataExtension.getTorrentFile(torrent);
		this.downloadFolderRoot = metadataExtension.getDownloadFolder();
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
			if(Arrays.equals(SHA1.hash(data), torrent.getMetadata().getHash())) {
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
