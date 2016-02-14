package org.johnnei.javatorrent.download.algos;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.protocol.messages.ut_metadata.MessageRequest;
import org.johnnei.javatorrent.torrent.download.Files;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.algos.AMetadataPhase;
import org.johnnei.javatorrent.torrent.download.algos.MetadataSelect;
import org.johnnei.javatorrent.torrent.download.files.Block;
import org.johnnei.javatorrent.torrent.download.files.Piece;
import org.johnnei.javatorrent.torrent.download.peer.Job;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.peer.PeerDirection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhaseMetadata extends AMetadataPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseMetadata.class);

	private final File downloadFolder;

	public PhaseMetadata(TorrentClient torrentClient, Torrent torrent, File metadataFile, File downloadFolder) {
		super(torrentClient, torrent, metadataFile);
		this.downloadFolder = downloadFolder;
	}

	@Override
	public boolean isDone() {
		return foundMatchingFile || torrent.getFiles().isDone();
	}

	@Override
	public void process() {
		for (Peer peer : getRelevantPeers(torrent.getPeers())) {
			Piece piece = torrent.getDownloadRegulator().getPieceForPeer(peer);
			if (piece == null) {
				continue;
			}
			while (piece.getRequestedCount() < piece.getBlockCount() && peer.getFreeWorkTime() > 0) {
				Block block = piece.getRequestBlock();
				if (block == null) {
					break;
				} else {
					Optional<PeerExtensions> peerExtensions = peer.getModuleInfo(PeerExtensions.class);
					if (!peerExtensions.isPresent()) {
						LOGGER.warn("Attempted to send metadata request to peer which doesn't support metadata. ", peer);
						continue;
					}

					IMessage message = new MessageExtension(peerExtensions.get().getExtensionId(UTMetadata.NAME), new MessageRequest(block.getIndex()));
					peer.addJob(new Job(piece.getIndex(), block.getIndex()), PeerDirection.Download);
					peer.getBitTorrentSocket().enqueueMessage(message);
				}
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
		torrent.setFiles(new Files(metadataFile, downloadFolder));
		LOGGER.info("Metadata download completed");
	}

	@Override
	public Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
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
