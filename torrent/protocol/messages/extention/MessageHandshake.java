package torrent.protocol.messages.extention;

import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.IMessage;

public class MessageHandshake implements IMessage {

	@Override
	public void write(Stream outStream) {
		// TODO Auto-generated method stub

	}

	@Override
	public void read(Stream inStream) {
		// TODO Auto-generated method stub

	}

	@Override
	public void process(Peer peer) {
		// TODO Auto-generated method stub

	}

	@Override
	public int getLength() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getId() {
		// TODO Auto-generated method stub
		return 0;
	}

}
