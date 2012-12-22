package torrent.protocol.messages.ut_metadata;

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
		MessageReject mr = new MessageReject((int)dictionary.get("piece"));
		MessageExtension extendedMessage = new MessageExtension(UTMetadata.ID, mr);
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

}
