package org.johnnei.javatorrent.phases;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.choking.PermissiveStrategy;
import org.johnnei.javatorrent.torrent.algos.pieceselector.FullPieceSelect;
import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhaseData implements IDownloadPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseData.class);

	private final Torrent torrent;

	private final TorrentClient torrentClient;

	private final IChokingStrategy chokingStrategy;

	public PhaseData(TorrentClient torrentClient, Torrent torrent) {
		this.torrentClient = torrentClient;
		this.torrent = torrent;
		chokingStrategy = new PermissiveStrategy();
	}

	@Override
	public boolean isDone() {
		return torrent.getFileSet().isDone();
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

				final Block block = blockOptional.get();
				peer.addBlockRequest(piece, torrent.getFileSet().getBlockSize() * block.getIndex(), block.getSize(), PeerDirection.Download);
			}
		}
	}

	@Override
	public void onPhaseEnter() {
		torrent.checkProgress();
		torrent.setPieceSelector(new FullPieceSelect(torrent));
		File downloadFolder = torrent.getFileSet().getDownloadFolder();

		if (!downloadFolder.exists() && !downloadFolder.mkdirs()) {
			throw new IllegalStateException(String.format("Failed to create download folder: %s", downloadFolder.getAbsolutePath()));
		}
	}

	@Override
	public void onPhaseExit() {
		torrentClient.getTrackersFor(torrent).forEach(tracker -> tracker.getInfo(torrent).get().setEvent(TrackerEvent.EVENT_COMPLETED));
		LOGGER.info("Download of {} completed", torrent);
	}

	Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
		Collection<Piece> neededPiece = torrent.getFileSet().getNeededPieces().collect(Collectors.toList());

		return peers.stream()
				.filter(peer -> neededPiece.stream().anyMatch(piece -> peer.hasPiece(piece.getIndex())))
				.collect(Collectors.toList());
	}

	@Override
	public IChokingStrategy getChokingStrategy() {
		return chokingStrategy;
	}
}
