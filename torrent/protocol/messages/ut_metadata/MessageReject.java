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
		int blockIndex = (int)dictionary.get("piece");
		peer.log("Piece Request got rejected: " + blockIndex , true);
		peer.getTorrent().getFiles().getPiece(0).reset(blockIndex);
		peer.getMyClient().removeJob(new Job(0, blockIndex));
	}

	@Override
	public int getLength() {
		return bencodedData.length();
	}

	@Override
	public int getId() {
		return UTMetadata.REJECT;
	}
	
	@Override
	public String toString() {
		return "UT_Metadata Reject";
	}

}
