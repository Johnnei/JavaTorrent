package org.johnnei.javatorrent.phases;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.algos.pieceselector.MetadataSelect;
import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.ut.metadata.protocol.UTMetadata;
import org.johnnei.javatorrent.ut.metadata.protocol.messages.MessageRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhaseMetadata extends AMetadataPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseMetadata.class);

	public PhaseMetadata(TorrentClient torrentClient, Torrent torrent) {
		super(torrentClient, torrent);
	}

	@Override
	public boolean isDone() {
		if (foundMatchingFile) {
			return true;
		}

		Optional<AbstractFileSet> metadataFileSet = torrent.getMetadata().getFileSet();
		if (!metadataFileSet.isPresent()) {
			return false;
		}
		return metadataFileSet.get().isDone();
	}

	@Override
	public void process() {
		for (Peer peer : getRelevantPeers(torrent.getPeers())) {
			Optional<Piece> pieceOptional = torrent.getPieceSelector().getPieceForPeer(peer);
			if (!pieceOptional.isPresent()) {
				continue;
			}

			Piece piece = pieceOptional.get();
			while (piece.hasBlockWithStatus(BlockStatus.Needed) && peer.getFreeWorkTime() > 0) {
				Optional<Block> blockOptional = piece.getRequestBlock();
				if (!blockOptional.isPresent()) {
					break;
				}

				// The getRelevantPeers call only returns peers which support the extensions and has ut_metadata.
				PeerExtensions peerExtensions = peer.getModuleInfo(PeerExtensions.class).get();
				Block block = blockOptional.get();
				IMessage message = new MessageExtension(peerExtensions.getExtensionId(UTMetadata.NAME), new MessageRequest(block.getIndex()));
				peer.getBitTorrentSocket().enqueueMessage(message);
			}
		}
	}

	@Override
	public void onPhaseEnter() {
		super.onPhaseEnter();
		torrent.setPieceSelector(new MetadataSelect(torrent));
	}

	@Override
	public void onPhaseExit() {
		if (!torrent.isDownloadingMetadata()) {
			return;
		}

		try (DataInputStream inputStream = new DataInputStream(new FileInputStream(metadataFile))) {
			byte[] buffer = new byte[(int) metadataFile.length()];
			inputStream.readFully(buffer);
			torrent.getMetadata().initializeMetadata(buffer);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("Metadata file has been removed after completion.", e);
		} catch (IOException e) {
			throw new TorrentException("Failed to read metadata", e);
		}

		torrent.setFileSet(new TorrentFileSet(torrent.getMetadata(), new File(downloadFolderRoot, torrent.getDisplayName())));
		LOGGER.info("Metadata download completed");
	}

	private Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
		return peers.stream()
				.filter(this::hasUtMetadataExtension)
				.collect(Collectors.toList());
	}

	private boolean hasUtMetadataExtension(Peer peer) {
		Optional<PeerExtensions> peerExtensions = peer.getModuleInfo(PeerExtensions.class);
		if (!peerExtensions.isPresent()) {
			return false;
		}
		return peerExtensions.get().hasExtension(UTMetadata.NAME);
	}
}
