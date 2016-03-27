package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * A message object which represents the KeepAlive case of the protocol. This message contains no actual payload information.
 */
public class MessageKeepAlive implements IMessage {

	@Override
	public void write(OutStream outStream) {
		/* No payload information to write */
	}

	@Override
	public void read(InStream inStream) {
		/* No payload information to read */
	}

	@Override
	public void process(Peer peer) {
		/* Peer activity is updated when this message was read from the socket */
	}

	@Override
	public int getLength() {
		/* Size is zero as nothing gets written, not even na ID */
		return 0;
	}

	@Override
	public int getId() {
		throw new UnsupportedOperationException("Keep alive messages don't send an ID and the protocol doesn't define the ID.");
	}

	@Override
	public String toString() {
		return "MessageKeepAlive[]";
	}

}
