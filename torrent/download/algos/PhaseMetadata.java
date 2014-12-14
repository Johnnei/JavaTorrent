package torrent.download.algos;

import org.johnnei.utils.config.Config;

import torrent.download.Files;
import torrent.download.Torrent;
import torrent.download.files.Block;
import torrent.download.files.Piece;
import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.download.peer.PeerDirection;
import torrent.download.tracker.TrackerManager;
import torrent.protocol.IMessage;
import torrent.protocol.UTMetadata;
import torrent.protocol.messages.extension.MessageExtension;
import torrent.protocol.messages.ut_metadata.MessageRequest;

public class PhaseMetadata extends AMetadataPhase {
	
	public PhaseMetadata(TrackerManager trackerManager, Torrent torrent) {
		super(trackerManager, torrent);
		this.trackerManager = trackerManager;
		this.torrent = torrent;
		foundMatchingFile = false;
	}
	
	@Override
	public boolean isDone() {
		return foundMatchingFile || torrent.getFiles().isDone();
	}

	@Override
	public IDownloadPhase nextPhase() {
		return new PhaseData(trackerManager, torrent);
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
	public void postprocess() {
		torrent.setFiles(new Files(Config.getConfig().getTorrentFileFor(torrent)));
		torrent.getLogger().info("Metadata download completed");
	}
}
