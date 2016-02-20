package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

public class MessageUninterested implements IMessage {

	@Override
	public void write(OutStream outStream) {
		/* Uninterested message has no payload */
	}

	@Override
	public void read(InStream inStream) {
		/* Uninterested message has no payload */
	}

	@Override
	public void process(Peer peer) {
		peer.setInterested(PeerDirection.Upload, false);
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_UNINTERESTED;
	}

	@Override
	public String toString() {
		return "MessageUninterested[]";
	}

}
