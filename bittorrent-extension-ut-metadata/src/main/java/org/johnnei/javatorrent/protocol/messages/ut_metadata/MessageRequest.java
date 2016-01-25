package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import org.johnnei.javatorrent.download.files.disk.DiskJobSendMetadataBlock;
import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;

import torrent.download.peer.Peer;

public class MessageRequest extends Message {

	public MessageRequest() {
	}

	public MessageRequest(int piece) {
		super(piece);
	}

	@Override
	public void process(Peer peer) {
		if (peer.getTorrent().isDownloadingMetadata()) {
			MessageReject mr = new MessageReject((int) dictionary.get("piece"));
			MessageExtension extendedMessage = new MessageExtension(peer.getExtensions().getIdFor(UTMetadata.NAME), mr);
			peer.getBitTorrentSocket().queueMessage(extendedMessage);
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
