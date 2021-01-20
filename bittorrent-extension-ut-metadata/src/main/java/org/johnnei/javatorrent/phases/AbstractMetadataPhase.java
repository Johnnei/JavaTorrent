package org.johnnei.javatorrent.phases;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.SHA1;
import org.johnnei.javatorrent.internal.ut.metadata.MetadataChocking;
import org.johnnei.javatorrent.module.MetadataInformation;
import org.johnnei.javatorrent.module.UTMetadataExtension;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.pieceselector.NopPrioritizer;
import org.johnnei.javatorrent.torrent.algos.pieceselector.PiecePrioritizer;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;

public abstract class AbstractMetadataPhase implements IDownloadPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractMetadataPhase.class);

	protected Torrent torrent;

	/**
	 * If the metadata file was found before this phase started
	 */
	protected boolean foundMatchingFile;

	protected TorrentClient torrentClient;

	protected final File metadataFile;

	protected final File downloadFolderRoot;

	private final IChokingStrategy chokingStrategy;

	private final PiecePrioritizer piecePrioritizer;

	public AbstractMetadataPhase(TorrentClient torrentClient, Torrent torrent) {
		this.torrentClient = torrentClient;
		this.torrent = torrent;
		this.piecePrioritizer = new NopPrioritizer();

		ExtensionModule extensionModule = torrentClient.getModule(ExtensionModule.class)
				.orElseThrow(() -> new IllegalStateException("Metadata phase registered without registering Extension module."));
		UTMetadataExtension metadataExtension = (UTMetadataExtension) extensionModule.getExtensionByName(UTMetadata.NAME)
				.orElseThrow(() -> new IllegalStateException("Metadata phase registered without registering ut_metadata extension."));

		this.metadataFile = metadataExtension.getTorrentFile(torrent);
		this.downloadFolderRoot = metadataExtension.getDownloadFolder();
		chokingStrategy = new MetadataChocking();
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

	@Override
	public boolean isPeerSupportedForDownload(Peer peer) {
		return peer.getModuleInfo(PeerExtensions.class).map(extensions -> extensions.hasExtension(UTMetadata.NAME)).orElse(false)
			&& peer.getModuleInfo(MetadataInformation.class).filter(metadata -> metadata.getMetadataSize() > 0).isPresent();
	}

	@Override
	public PiecePrioritizer getPiecePrioritizer() {
		return piecePrioritizer;
	}
}
