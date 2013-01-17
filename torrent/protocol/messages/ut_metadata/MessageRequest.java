package torrent.protocol.messages.ut_metadata;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.protocol.UTMetadata;
import torrent.protocol.messages.extention.MessageExtension;

public class MessageRequest extends Message {
	
	public MessageRequest() {
	}
	
	public MessageRequest(int piece) {
		super(piece);
	}

	@Override
	public void process(Peer peer) {
		MessageExtension extendedMessage;
		if(peer.getTorrent().getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA) {
			MessageReject mr = new MessageReject((int)dictionary.get("piece"));
			extendedMessage = new MessageExtension(peer.getClient().getExtentionID(UTMetadata.NAME), mr);
			peer.addToQueue(extendedMessage);
		} else {
			int piece = (int)dictionary.get("piece");
			MessageData mData = new MessageData(piece, peer.getTorrent().getFiles().getMetadataBlock(piece));
			extendedMessage = new MessageExtension(peer.getClient().getExtentionID(UTMetadata.NAME), mData);
		}
		peer.addToQueue(extendedMessage);
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
