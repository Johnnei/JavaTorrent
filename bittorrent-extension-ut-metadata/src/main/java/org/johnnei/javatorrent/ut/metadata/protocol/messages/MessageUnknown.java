package org.johnnei.javatorrent.ut.metadata.protocol.messages;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * Implementation for the ut_metadata extension of message which is basically a 'null' message which swallows all unknown messages as defined per spec.
 */
public class MessageUnknown implements IMessage {

	private final static String ERR_MESSAGE = "This message can not be written, we don't know what it is!";

	@Override
	public void write(OutStream outStream) {
		throw new UnsupportedOperationException(ERR_MESSAGE);
	}

	@Override
	public void read(InStream inStream) {
		// Nothing to read as we don't know what we are expecting here
	}

	@Override
	public void process(Peer peer) {
		// Don't know what it is, so can't process anything.
	}

	@Override
	public int getLength() {
		throw new UnsupportedOperationException(ERR_MESSAGE);
	}

	@Override
	public int getId() {
		throw new UnsupportedOperationException(ERR_MESSAGE);
	}
}
