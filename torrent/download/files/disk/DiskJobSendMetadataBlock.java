package torrent.download.files.disk;

import java.io.IOException;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.protocol.UTMetadata;
import torrent.protocol.messages.extension.MessageExtension;
import torrent.protocol.messages.ut_metadata.MessageData;

public class DiskJobSendMetadataBlock extends DiskJob {

	/**
	 * The peer to send the block to
	 */
	private Peer peer;
	private int blockIndex;

	public DiskJobSendMetadataBlock(Peer peer, int blockIndex) {
		this.blockIndex = blockIndex;
		this.peer = peer;
	}

	@Override
	public void process(Torrent torrent) {
		try {
			MessageData mData = new MessageData(blockIndex, peer.getTorrent().getMetadata().getBlock(blockIndex));
			MessageExtension extendedMessage = new MessageExtension(peer.getExtensions().getIdFor(UTMetadata.NAME), mData);
			peer.getBitTorrentSocket().queueMessage(extendedMessage);
		} catch (IOException e) {
			torrent.getLogger().warning(String.format("Reading metadata block %d failed, requeueing read job. %s", blockIndex, e.getMessage()));
			torrent.addDiskJob(new DiskJobSendMetadataBlock(peer, blockIndex));
		}
	}

	@Override
	public int getPriority() {
		return NORMAL;
	}

}
