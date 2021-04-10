package org.johnnei.javatorrent.phases;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.ut.metadata.UtMetadata;
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

		try {
			torrent.setMetadata(UtMetadata.from(metadataFile).build());
		} catch (IOException e) {
			throw new TorrentException("Failed to read metadata", e);
		}

		torrent.setFileSet(new TorrentFileSet(torrent.getMetadata(), downloadFolderRoot.resolve(torrent.getDisplayName()).toFile()));
	}

	@Override
	public Optional<AbstractFileSet> getFileSet() {
		return torrent.getMetadata().getFileSet();
	}
}
