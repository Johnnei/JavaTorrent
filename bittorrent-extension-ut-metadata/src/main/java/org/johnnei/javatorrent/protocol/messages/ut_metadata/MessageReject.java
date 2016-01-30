package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.torrent.download.peer.Job;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.peer.PeerDirection;

public class MessageReject extends Message {

	public MessageReject() {
		super();
	}

	public MessageReject(int piece) {
		super(piece);
	}

	@Override
	public void process(Peer peer) {
		int blockIndex = (int) dictionary.get("piece");
		peer.getLogger().severe("Piece Request got rejected: " + blockIndex);
		peer.getTorrent().getFiles().getPiece(0).reset(blockIndex);
		peer.removeJob(new Job(0, blockIndex), PeerDirection.Download);
		peer.getBitTorrentSocket().close();
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
