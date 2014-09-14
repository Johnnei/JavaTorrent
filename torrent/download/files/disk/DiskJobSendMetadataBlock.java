package torrent.download.files.disk;

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
		MessageData mData = new MessageData(blockIndex, peer.getTorrent().getFiles().getMetadataBlock(blockIndex));
		MessageExtension extendedMessage = new MessageExtension(peer.getExtensions().getIdFor(UTMetadata.NAME), mData);
		peer.addToQueue(extendedMessage);
	}

	@Override
	public int getPriority() {
		return NORMAL;
	}

}
