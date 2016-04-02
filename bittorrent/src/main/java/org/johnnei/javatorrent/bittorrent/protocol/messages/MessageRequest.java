package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.internal.bitorrent.protocol.messages.AbstractBlockMessage;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageRequest extends AbstractBlockMessage implements IMessage {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageRequest.class);

	public MessageRequest() {
		/* Empty constructor for receiving messages */
	}

	public MessageRequest(int index, int offset, int length) {
		this.index = index;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public void process(Peer peer) {
		if (peer.getTorrent().getFileSet().hasPiece(index)) {
			peer.addBlockRequest(index, offset, length, PeerDirection.Upload);
		} else {
			LOGGER.error("Requested piece {} which I don't have", index);
			peer.getBitTorrentSocket().close();
		}
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_REQUEST;
	}

	@Override
	public String toString() {
		return String.format("MessageRequest[index=%d, offset=%d, lenght=%d]", index, offset, length);
	}

}
