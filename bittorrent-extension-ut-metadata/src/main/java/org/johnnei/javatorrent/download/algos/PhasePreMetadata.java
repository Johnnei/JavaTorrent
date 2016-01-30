package org.johnnei.javatorrent.download.algos;

import java.util.Collection;
import java.util.Collections;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.MetadataFile;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.algos.AMetadataPhase;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.utils.config.Config;

public class PhasePreMetadata extends AMetadataPhase {

	private int metadataSize;

	public PhasePreMetadata(TorrentClient torrentClient, Torrent torrent) {
		super(torrentClient, torrent);
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
