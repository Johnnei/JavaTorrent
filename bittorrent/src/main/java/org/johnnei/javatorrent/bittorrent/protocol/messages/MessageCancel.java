package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.internal.bitorrent.protocol.messages.AbstractBlockMessage;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

public class MessageCancel extends AbstractBlockMessage implements IMessage {

	public MessageCancel() {
		/* Constructor to receive packets */
	}

	public MessageCancel(int index, int begin, int offset) {
		this.index = index;
		this.offset = begin;
		this.length = offset;
	}

	@Override
	public void process(Peer peer) {
		peer.cancelBlockRequest(index, offset, length, PeerDirection.Upload);
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_CANCEL;
	}

	@Override
	public String toString() {
		return String.format("MessageCancel[index=%d, offset=%d, lenght=%d]", index, offset, length);
	}

}
