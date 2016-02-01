package org.johnnei.javatorrent.download.algos;

import java.util.Collection;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.protocol.UTMetadata;
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
import org.johnnei.javatorrent.utils.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhaseMetadata extends AMetadataPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseMetadata.class);

	public PhaseMetadata(TorrentClient torrentClient, Torrent torrent) {
		super(torrentClient, torrent);
		foundMatchingFile = false;
	}

	@Override
	public boolean isDone() {
		return foundMatchingFile || torrent.getFiles().isDone();
	}

	@Override
	public void process() {
		for (Peer peer : torrent.getDownloadablePeers()) {
			Piece piece = torrent.getDownloadRegulator().getPieceForPeer(peer);
			if (piece == null) {
				continue;
			}
			while (piece.getRequestedCount() < piece.getBlockCount() && peer.getFreeWorkTime() > 0) {
				Block block = piece.getRequestBlock();
				if (block == null) {
					break;
				} else {
					IMessage message = new MessageExtension(peer.getExtensions().getIdFor(UTMetadata.NAME), new MessageRequest(block.getIndex()));
					peer.addJob(new Job(piece.getIndex(), block.getIndex()), PeerDirection.Download);
					peer.getBitTorrentSocket().queueMessage(message);
				}
			}
		}
	}

	@Override
	public void onPhaseEnter() {
		super.onPhaseEnter();
		torrent.setDownloadRegulator(new MetadataSelect(torrent));
	}

	@Override
	public void onPhaseExit() {
		torrent.setFiles(new Files(Config.getConfig().getTorrentFileFor(torrent.getHash())));
		LOGGER.info("Metadata download completed");
	}

	@Override
	public Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
		return peers.stream()
				.filter(peer -> peer.getExtensions().hasExtension(UTMetadata.NAME))
				.collect(Collectors.toList());
	}
}
