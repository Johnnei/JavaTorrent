package torrent.download.algos;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.johnnei.utils.config.Config;

import torrent.download.FileInfo;
import torrent.download.Files;
import torrent.download.Torrent;
import torrent.download.files.Block;
import torrent.download.files.Piece;
import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.encoding.SHA1;
import torrent.protocol.IMessage;
import torrent.protocol.UTMetadata;
import torrent.protocol.messages.extension.MessageExtension;
import torrent.protocol.messages.ut_metadata.MessageRequest;

public class PhaseMetadata implements IDownloadPhase {

	private Torrent torrent;
	/**
	 * If the metadata file was found before this phase started
	 */
	private boolean foundMatchingFile;
	
	public PhaseMetadata(Torrent torrent) {
		this.torrent = torrent;
		foundMatchingFile = false;
	}
	
	@Override
	public boolean isDone() {
		return foundMatchingFile || torrent.getFiles().isDone();
	}

	@Override
	public IDownloadPhase nextPhase() {
		return new PhaseData(torrent);
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
					IMessage message = new MessageExtension(peer.getClient().getExtentionID(UTMetadata.NAME), new MessageRequest(block.getIndex()));
					peer.getMyClient().addJob(new Job(piece.getIndex(), block.getIndex()));
					peer.addToQueue(message);
				}
			}
		}
	}

	@Override
	public void preprocess() {
		FileInfo metadataFile = torrent.getFiles().getFiles()[0];
		File file = new File(Config.getConfig().getTempFolder() + metadataFile.getFilename());
		if(file.exists()) {
			synchronized (metadataFile.FILE_LOCK) {
				RandomAccessFile fileAccess = metadataFile.getFileAcces();
				try {
					byte[] data = new byte[(int)fileAccess.length()];
					fileAccess.seek(0);
					fileAccess.read(data, 0, data.length);
					if(SHA1.match(SHA1.hash(data), torrent.getHashArray())) {
						foundMatchingFile = true;
						torrent.log("Found pre-downloaded Torrent file");
					}
				} catch (IOException e) {
				}
			}
		}
	}

	@Override
	public void postprocess() {
		FileInfo f = torrent.getFiles().getFiles()[0];
		torrent.setFiles(new Files(new File(Config.getConfig().getTempFolder() + f.getFilename())));
		torrent.log("Metadata download completed");
	}

	@Override
	public byte getId() {
		return Torrent.STATE_DOWNLOAD_METADATA;
	}
}
