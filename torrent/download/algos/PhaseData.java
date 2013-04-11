package torrent.download.algos;

import java.util.ArrayList;

import torrent.download.Torrent;
import torrent.download.files.Block;
import torrent.download.files.Piece;
import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.download.tracker.Tracker;
import torrent.download.tracker.TrackerConnection;
import torrent.protocol.IMessage;
import torrent.protocol.messages.MessageRequest;

public class PhaseData implements IDownloadPhase {

	private Torrent torrent;
	
	public PhaseData(Torrent torrent) {
		this.torrent = torrent;
	}
	
	@Override
	public boolean isDone() {
		return torrent.getFiles().isDone();
	}

	@Override
	public IDownloadPhase nextPhase() {
		return new PhaseUpload(torrent);
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
					peer.getMyClient().addJob(new Job(piece.getIndex(), block.getIndex()));
					peer.addToQueue(message);
				}
			}
		}
	}

	@Override
	public void preprocess() {
		torrent.updateBitfield();
		torrent.checkProgress();
	}

	@Override
	public void postprocess() {
		for(Tracker tracker : torrent.getTrackers()) {
			tracker.getInfo(torrent).setEvent(TrackerConnection.EVENT_COMPLETED);
		}
		torrent.log("Download completed");
	}

	@Override
	public byte getId() {
		return Torrent.STATE_DOWNLOAD_DATA;
	}
}
