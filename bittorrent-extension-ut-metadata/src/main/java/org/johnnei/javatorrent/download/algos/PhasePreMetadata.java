package org.johnnei.javatorrent.download.algos;

import java.io.File;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;

/**
 * The phase in which the torrent needs to wait to connect to peers. Upon handshake with the peers the ut_metadata extension will allow us to discover the
 * torrent file size. Once the torrent file size is known we can advance to the next phase and start downloading the torrent file.
 */
public class PhasePreMetadata extends AMetadataPhase {

	public PhasePreMetadata(TorrentClient torrentClient, Torrent torrent, File metadataFile) {
		super(torrentClient, torrent, metadataFile);
	}

	@Override
	public boolean isDone() {
		return torrent.getPeers().stream().anyMatch(p -> p.getModuleInfo(MetadataInformation.class).isPresent());
	}

	@Override
	public void process() {
		// Wait for peers to connect with the correct information.
	}

	@Override
	public void onPhaseExit() {
		MetadataFileSet metadata = new MetadataFileSet(torrent, metadataFile);
		torrent.setMetadata(metadata);
	}

}
