package org.johnnei.javatorrent.download.files.disk;

import java.io.IOException;

import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.protocol.messages.ut_metadata.MessageData;

import torrent.download.Torrent;
import torrent.download.files.disk.DiskJob;
import torrent.download.peer.Peer;

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
			MessageData mData = new MessageData(blockIndex, peer.getTorrent().getMetadata().get().getBlock(blockIndex));
			MessageExtension extendedMessage = new MessageExtension(peer.getExtensions().getIdFor(UTMetadata.NAME), mData);
			peer.getBitTorrentSocket().queueMessage(extendedMessage);
		} catch (IOException e) {
			torrent.getLogger().warning(String.format("Reading metadata block %d failed, requeueing read job. %s", blockIndex, e.getMessage()));
			torrent.addDiskJob(this);
		}
	}

	@Override
	public int getPriority() {
		return NORMAL;
	}

}
