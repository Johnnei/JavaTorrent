package torrent.download.algos;

import java.util.ArrayList;
import java.util.Collection;

import org.johnnei.javatorrent.network.protocol.IMessage;

import torrent.download.Torrent;
import torrent.download.files.Block;
import torrent.download.files.Piece;
import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.download.peer.PeerDirection;
import torrent.download.tracker.TrackerConnection;
import torrent.download.tracker.TrackerManager;
import torrent.protocol.messages.MessageRequest;

public class PhaseData implements IDownloadPhase {

	private Torrent torrent;
	private TrackerManager trackerManager;

	public PhaseData(TrackerManager trackerManager, Torrent torrent) {
		this.trackerManager = trackerManager;
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
		torrent.setDownloadRegulator(new FullPieceSelect(torrent));
	}

	@Override
	public void onPhaseExit() {
		trackerManager.getTrackersFor(torrent).forEach(tracker -> tracker.getInfo(torrent).setEvent(TrackerConnection.EVENT_COMPLETED));
		torrent.getLogger().info("Download completed");
	}

	@Override
	public Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
		// TODO Auto-generated method stub
		return null;
	}
}
