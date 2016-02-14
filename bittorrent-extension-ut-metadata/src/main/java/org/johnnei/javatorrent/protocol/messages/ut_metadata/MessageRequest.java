package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import java.util.Optional;

import org.johnnei.javatorrent.download.files.disk.DiskJobSendMetadataBlock;
import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageRequest extends Message {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageRequest.class);

	public MessageRequest() {
	}

	public MessageRequest(int piece) {
		super(piece);
	}

	@Override
	public void process(Peer peer) {
		if (peer.getTorrent().isDownloadingMetadata()) {
			Optional<PeerExtensions> peerExtensions = peer.getModuleInfo(PeerExtensions.class);
			if (!peerExtensions.isPresent() || !peerExtensions.get().hasExtension(UTMetadata.NAME)) {
				LOGGER.warn("Request to send Metadata block to {} has been rejected. Peer doesn't know about UT_METADATA", peer);
				return;
			}

			MessageReject mr = new MessageReject((int) dictionary.get("piece"));
			MessageExtension extendedMessage = new MessageExtension(peerExtensions.get().getExtensionId(UTMetadata.NAME), mr);
			peer.getBitTorrentSocket().enqueueMessage(extendedMessage);
		} else {
			int piece = (int) dictionary.get("piece");
			peer.getTorrent().addDiskJob(new DiskJobSendMetadataBlock(peer, piece));
		}
	}

	@Override
	public int getLength() {
		return bencodedData.length();
	}

	@Override
	public int getId() {
		return UTMetadata.REQUEST;
	}

	@Override
	public String toString() {
		return "UT_Metadata Request";
	}

}
