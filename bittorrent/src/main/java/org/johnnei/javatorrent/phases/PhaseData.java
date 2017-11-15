package org.johnnei.javatorrent.phases;

import java.io.File;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.choking.PermissiveStrategy;
import org.johnnei.javatorrent.torrent.algos.pieceselector.FullPieceSelect;
import org.johnnei.javatorrent.torrent.files.Block;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

/**
 * The download phase in which the actual torrent files will be downloaded.
 */
public class PhaseData implements IDownloadPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseData.class);

	private final Torrent torrent;

	private final TorrentClient torrentClient;

	private final IChokingStrategy chokingStrategy;

	/**
	 * Creates a new Data Phase for the given torrent.
	 * @param torrentClient The client used to notify trackers.
	 * @param torrent The torrent which we are downloading.
	 */
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
		getRelevantPeers(torrent.getPeers()).forEach(peer -> {
			while (peer.getFreeWorkTime() > 0) {
				Optional<Piece> piece = torrent.getPieceSelector().getPieceForPeer(peer);
				if (piece.isPresent()) {
					requestBlocksOfPiece(peer, piece.get());
				} else {
					// Stop processing for this peer when no more pieces are available.
					break;
				}
			}
		});
	}

	private void requestBlocksOfPiece(Peer peer, Piece piece) {
		while (peer.getFreeWorkTime() > 0) {
			Optional<Block> blockOptional = piece.getRequestBlock();
			if (!blockOptional.isPresent()) {
				break;
			}

			final Block block = blockOptional.get();
			peer.addBlockRequest(piece, torrent.getFileSet().getBlockSize() * block.getIndex(), block.getSize(), PeerDirection.Download);
		}
	}

	@Override
	public void onPhaseEnter() {
		torrent.checkProgress();
		torrent.setPieceSelector(new FullPieceSelect(torrent));
		File downloadFolder = torrent.getFileSet().getDownloadFolder();

		if (!downloadFolder.exists() && !downloadFolder.mkdirs()) {
			throw new TorrentException(String.format("Failed to create download folder: %s", downloadFolder.getAbsolutePath()));
		}
	}

	@Override
	public void onPhaseExit() {
		torrentClient.getTrackersFor(torrent).forEach(tracker -> tracker.getInfo(torrent).get().setEvent(TrackerEvent.EVENT_COMPLETED));
		LOGGER.info("Download of {} completed", torrent);
	}

	Stream<Peer> getRelevantPeers(Collection<Peer> peers) {
		Collection<Piece> neededPiece = torrent.getFileSet().getNeededPieces().collect(Collectors.toList());

		return peers.stream()
			.filter(peer -> !peer.isChoked(PeerDirection.Download))
			.filter(peer -> neededPiece.stream().anyMatch(piece -> peer.hasPiece(piece.getIndex())));
	}

	@Override
	public IChokingStrategy getChokingStrategy() {
		return chokingStrategy;
	}
}
