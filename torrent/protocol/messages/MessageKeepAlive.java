package torrent.protocol.messages;

import torrent.download.peer.Peer;
import torrent.network.InStream;
import torrent.network.OutStream;
import torrent.protocol.IMessage;

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
