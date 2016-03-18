package org.johnnei.javatorrent.download.algos;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class PhasePreMetadata extends AMetadataPhase {

	private int metadataSize;

	public PhasePreMetadata(TorrentClient torrentClient, Torrent torrent, File metadataFile) {
		super(torrentClient, torrent, metadataFile);
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
			metadataSize = (int) metadataFile.length();
		}
	}

	@Override
	public void onPhaseExit() {
		MetadataFileSet metadata = new MetadataFileSet(torrent, metadataFile);
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
