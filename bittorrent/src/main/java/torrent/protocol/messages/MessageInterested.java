package torrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;

import torrent.download.peer.Peer;
import torrent.download.peer.PeerDirection;
import torrent.protocol.BitTorrent;

public class MessageInterested implements IMessage {

	@Override
	public void write(OutStream outStream) {
	}

	@Override
	public void read(InStream inStream) {
	}

	@Override
	public void process(Peer peer) {
		// This is questionable behaviour, they notify that THEY are interested, for us that is upload not download
		peer.setInterested(PeerDirection.Download, true);
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_INTERESTED;
	}

	@Override
	public void setReadDuration(int duration) {
	}

	@Override
	public String toString() {
		return "Interested";
	}

}
