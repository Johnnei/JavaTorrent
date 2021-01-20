package org.johnnei.javatorrent.phases;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.TorrentFileSet;

public class DownloadMetadataPhase extends AbstractMetadataPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(DownloadMetadataPhase.class);

	public DownloadMetadataPhase(TorrentClient torrentClient, Torrent torrent) {
		super(torrentClient, torrent);
	}

	@Override
	public boolean isDone() {
		if (foundMatchingFile) {
			return true;
		}

		return torrent.getMetadata().getFileSet().map(AbstractFileSet::isDone).orElse(false);
	}

	@Override
	public void process() {
	}

	@Override
	public void onPhaseExit() {
		LOGGER.info("Metadata download completed");
		if (!torrent.isDownloadingMetadata()) {
			return;
		}

		try (DataInputStream inputStream = new DataInputStream(new FileInputStream(metadataFile))) {
			byte[] buffer = new byte[(int) metadataFile.length()];
			inputStream.readFully(buffer);
			torrent.getMetadata().initializeMetadata(buffer);
		} catch (FileNotFoundException e) {
			throw new TorrentException("Metadata file has been removed after completion.", e);
		} catch (IOException e) {
			throw new TorrentException("Failed to read metadata", e);
		}

		torrent.setFileSet(new TorrentFileSet(torrent.getMetadata(), new File(downloadFolderRoot, torrent.getDisplayName())));
	}

	@Override
	public Optional<AbstractFileSet> getFileSet() {
		return torrent.getMetadata().getFileSet();
	}
}
