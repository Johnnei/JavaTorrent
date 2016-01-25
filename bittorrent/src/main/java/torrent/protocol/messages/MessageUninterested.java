package torrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;

import torrent.download.peer.Peer;
import torrent.download.peer.PeerDirection;
import torrent.protocol.BitTorrent;

public class MessageUninterested implements IMessage {

	@Override
	public void write(OutStream outStream) {
	}

	@Override
	public void read(InStream inStream) {
	}

	@Override
	public void process(Peer peer) {
		// Again, doubt full about model-correctness
		peer.setInterested(PeerDirection.Download, false);
	}

	@Override
	public int getLength() {
		return 1;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_UNINTERESTED;
	}

	@Override
	public void setReadDuration(int duration) {
	}

	@Override
	public String toString() {
		return "Uninterested";
	}

}
