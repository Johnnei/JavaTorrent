package org.johnnei.javatorrent.phases;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentException;

/**
 * The phase in which the torrent needs to wait to connect to peers. Upon handshake with the peers the ut_metadata extension will allow us to discover the
 * torrent file size. Once the torrent file size is known we can advance to the next phase and start downloading the torrent file.
 */
public class PhasePreMetadata extends AMetadataPhase {

	private int fileSize;

	public PhasePreMetadata(TorrentClient torrentClient, Torrent torrent) {
		super(torrentClient, torrent);
	}

	@Override
	public boolean isDone() {
		Optional<MetadataInformation> info = torrent.getPeers().stream()
				.map(p -> p.getModuleInfo(MetadataInformation.class))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findAny();

		if (!info.isPresent()) {
			return false;
		}

		fileSize = info.get().getMetadataSize();
		return true;
	}

	@Override
	public void process() {
		// Wait for peers to connect with the correct information.
	}

	@Override
	public void onPhaseExit() {
		if (metadataFile.length() != fileSize) {
			try (RandomAccessFile fileAccess = new RandomAccessFile(metadataFile, "rw")){
				fileAccess.setLength(fileSize);
			} catch (IOException e) {
				throw new TorrentException("Failed to allocate space for the metadata file", e);
			}
		}

		MetadataFileSet metadata = new MetadataFileSet(torrent, metadataFile);
		torrent.setMetadata(metadata);
	}

}
