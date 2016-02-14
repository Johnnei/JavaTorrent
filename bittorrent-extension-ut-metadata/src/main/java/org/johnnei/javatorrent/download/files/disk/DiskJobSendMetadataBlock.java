package org.johnnei.javatorrent.download.files.disk;

import java.io.IOException;
import java.util.Optional;

import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
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
			Optional<PeerExtensions> peerExtensions = peer.getModuleInfo(PeerExtensions.class);
			if (!peerExtensions.isPresent() || !peerExtensions.get().hasExtension(UTMetadata.NAME)) {
				LOGGER.warn("Request to send Metadata block {} to {} has been rejected. Peer doesn't know about UT_METADATA", blockIndex, peer);
				return;
			}

			MessageData mData = new MessageData(blockIndex, peer.getTorrent().getMetadata().get().getBlock(blockIndex));
			MessageExtension extendedMessage = new MessageExtension(peerExtensions.get().getExtensionId(UTMetadata.NAME), mData);
			peer.getBitTorrentSocket().enqueueMessage(extendedMessage);
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
