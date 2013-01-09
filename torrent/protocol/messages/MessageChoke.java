package torrent.protocol.messages;

import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;

public class MessageChoke implements IMessage {

	@Override
	public void write(Stream outStream) {
	}

	@Override
	public void read(Stream inStream) {
	}

	@Override
	public void process(Peer peer) {
		peer.getMyClient().choke();
		peer.cancelAllPieces();
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_CHOKE;
	}
	
	@Override
	public void setReadDuration(int duration) {
	}
	
	@Override
	public String toString() {
		return "Choke";
	}

}
