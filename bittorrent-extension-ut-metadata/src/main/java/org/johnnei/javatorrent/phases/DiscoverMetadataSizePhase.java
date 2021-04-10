package org.johnnei.javatorrent.phases;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentException;

/**
 * The phase in which the torrent needs to wait to connect to peers.
 * Upon handshake with the peers the ut_metadata extension will allow us to discover the torrent file size.
 * Once the torrent file size is known we can advance to the next phase and start downloading the torrent file.
 */
public class DiscoverMetadataSizePhase extends AbstractMetadataPhase {

	private long fileSize;

	public DiscoverMetadataSizePhase(TorrentClient torrentClient, Torrent torrent) {
		super(torrentClient, torrent);
	}

	@Override
	public boolean isDone() {
		if (foundMatchingFile) {
			return true;
		}

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
	public void onPhaseEnter() {
		super.onPhaseEnter();
		if (foundMatchingFile) {
			try {
				fileSize = (int) Files.size(metadataFile);
			} catch (IOException e) {
				throw new TorrentException("Failed to read file size of " + metadataFile.toAbsolutePath(), e);
			}
		}
	}

	@Override
	public void onPhaseExit() {
		try {
			if (Files.notExists(metadataFile) || Files.size(metadataFile) != fileSize) {
				try (SeekableByteChannel channel = Files.newByteChannel(metadataFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
					channel.truncate(fileSize);
					channel.position(fileSize - 1);
					channel.write(ByteBuffer.wrap(new byte[] { 0 }));
				}

				Metadata metadataWithFileset = new Metadata.Builder(torrent.getMetadata())
					.withFileSet(new MetadataFileSet(torrent.getMetadata().getHash(), metadataFile))
					.build();

				torrent.setMetadata(metadataWithFileset);
			}
		} catch (IOException e) {
			throw new TorrentException("Failed to allocate space for the metadata file: " + metadataFile.toAbsolutePath(), e);
		}
	}

	@Override
	public Optional<AbstractFileSet> getFileSet() {
		return Optional.empty();
	}
}
