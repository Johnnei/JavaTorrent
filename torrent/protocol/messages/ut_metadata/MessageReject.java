package torrent.protocol.messages.ut_metadata;

import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.protocol.UTMetadata;

public class MessageReject extends Message {
	
	public MessageReject() {
		super();
	}
	
	public MessageReject(int piece) {
		super(piece);
	}

	@Override
	public void process(Peer peer) {
		peer.log("Piece Request got rejected: " + dictionary.get("piece"), true);
		peer.getTorrent().collectPiece((int) dictionary.get("piece"), null);
		peer.removeFromQueue(new Job(-1 - (int)dictionary.get("piece")));
	}

	@Override
	public int getLength() {
		return bencodedData.length();
	}

	@Override
	public int getId() {
		return UTMetadata.REJECT;
	}

}
