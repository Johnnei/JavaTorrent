package org.johnnei.javatorrent.download.files.disk;

import java.io.IOException;

import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.protocol.messages.ut_metadata.MessageData;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.files.disk.DiskJob;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiskJobSendMetadataBlock extends DiskJob {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiskJobSendMetadataBlock.class);

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
			MessageData mData = new MessageData(blockIndex, peer.getTorrent().getMetadata().get().getBlock(blockIndex));
			MessageExtension extendedMessage = new MessageExtension(peer.getExtensions().getIdFor(UTMetadata.NAME), mData);
			peer.getBitTorrentSocket().queueMessage(extendedMessage);
		} catch (IOException e) {
			LOGGER.warn(String.format("Reading metadata block %d failed, requeueing read job.", blockIndex), e);
			torrent.addDiskJob(this);
		}
	}

	@Override
	public int getPriority() {
		return NORMAL;
	}

}
