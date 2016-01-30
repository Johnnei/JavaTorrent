package org.johnnei.javatorrent.torrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

public class MessageKeepAlive implements IMessage {

	@Override
	public void write(OutStream outStream) {
	}

	@Override
	public void read(InStream inStream) {
	}

	@Override
	public void process(Peer peer) {
		peer.updateLastActivity();
	}

	@Override
	public int getLength() {
		return 0;
	}

	@Override
	public int getId() {
		return 0;
	}

	@Override
	public void setReadDuration(int duration) {
	}

	@Override
	public String toString() {
		return "Keep alive";
	}

}
