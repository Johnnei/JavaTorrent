package torrent.protocol.messages;

import torrent.download.peer.Peer;
import torrent.download.peer.PeerDirection;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;

public class MessageUnchoke implements IMessage {

	@Override
	public void write(Stream outStream) {
	}

	@Override
	public void read(Stream inStream) {
	}

	@Override
	public void process(Peer peer) {
		peer.setChoked(PeerDirection.Download, false);
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_UNCHOKE;
	}

	@Override
	public void setReadDuration(int duration) {
	}

	@Override
	public String toString() {
		return "Unchoke";
	}

}
