package org.johnnei.javatorrent.download.algos;

import java.util.Collection;
import java.util.Collections;

import org.johnnei.utils.config.Config;

import torrent.download.MetadataFile;
import torrent.download.Torrent;
import torrent.download.algos.AMetadataPhase;
import torrent.download.peer.Peer;
import torrent.download.tracker.TrackerManager;

public class PhasePreMetadata extends AMetadataPhase {

	private int metadataSize;

	public PhasePreMetadata(TrackerManager trackerManager, Torrent torrent) {
		super(trackerManager, torrent);
	}

	@Override
	public boolean isDone() {
		return metadataSize != 0;
	}

	@Override
	public void process() {
		// Wait for peers to connect with the correct information.
	}

	@Override
	public void onPhaseEnter() {
		super.onPhaseEnter();
		if (foundMatchingFile) {
			metadataSize = (int) Config.getConfig().getTorrentFileFor(torrent.getHash()).length();
		}
	}

	@Override
	public void onPhaseExit() {
		MetadataFile metadata = new MetadataFile(torrent, metadataSize);
		torrent.setFiles(metadata);
		torrent.setMetadata(metadata);
	}

	public void setMetadataSize(int metadataSize) {
		this.metadataSize = metadataSize;
	}

	@Override
	public Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
		// We don't really have any 'useful' here, we're just waiting until we get our information
		return Collections.emptyList();
	}

}
