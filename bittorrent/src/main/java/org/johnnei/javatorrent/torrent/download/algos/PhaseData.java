package org.johnnei.javatorrent.torrent.download.algos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.files.Block;
import org.johnnei.javatorrent.torrent.download.files.Piece;
import org.johnnei.javatorrent.torrent.download.peer.Job;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.peer.PeerDirection;
import org.johnnei.javatorrent.torrent.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.torrent.tracker.TrackerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhaseData implements IDownloadPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseData.class);

	private Torrent torrent;
	private TorrentClient torrentClient;

	public PhaseData(TorrentClient torrentClient, Torrent torrent) {
		this.torrentClient = torrentClient;
		this.torrent = torrent;
	}

	@Override
	public boolean isDone() {
		return torrent.getFiles().isDone();
	}

	@Override
	public void process() {
		ArrayList<Peer> downloadPeers = torrent.getDownloadablePeers();
		while(downloadPeers.size() > 0) {
			Peer peer = downloadPeers.remove(0);
			Piece piece = torrent.getDownloadRegulator().getPieceForPeer(peer);
			if (piece == null) {
				continue;
			}
			while (piece.getRequestedCount() < piece.getBlockCount() && peer.getFreeWorkTime() > 0) {
				Block block = piece.getRequestBlock();
				if (block == null) {
					break;
				} else {
					IMessage message = new MessageRequest(piece.getIndex(), block.getIndex() * torrent.getFiles().getBlockSize(), block.getSize());
					peer.addJob(new Job(piece.getIndex(), block.getIndex()), PeerDirection.Download);
					peer.getBitTorrentSocket().queueMessage(message);
				}
			}
		}
	}

	@Override
	public void onPhaseEnter() {
		torrent.checkProgress();
		torrent.setPieceSelector(new FullPieceSelect(torrent));
	}

	@Override
	public void onPhaseExit() {
		torrentClient.getTrackerManager().getTrackersFor(torrent).forEach(tracker -> tracker.getInfo(torrent).get().setEvent(TrackerEvent.EVENT_COMPLETED));
		LOGGER.info("Download completed");
	}

	@Override
	public Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
		Collection<Piece> neededPiece = torrent.getFiles().getNeededPieces().collect(Collectors.toList());

		return peers.stream()
				.filter(peer -> neededPiece.stream().anyMatch(piece -> peer.hasPiece(piece.getIndex())))
				.collect(Collectors.toList());
	}
}
