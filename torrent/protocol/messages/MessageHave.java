package torrent.protocol.messages;

import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;

public class MessageHave implements IMessage {
	
	private int pieceIndex;
	
	public MessageHave() {
	}
	
	public MessageHave(int pieceIndex) {
		this.pieceIndex = pieceIndex;
	}

	@Override
	public void write(Stream outStream) {
		outStream.writeInt(pieceIndex);
	}

	@Override
	public void read(Stream inStream) {
		pieceIndex = inStream.readInt();
	}

	@Override
	public void process(Peer peer) {
		peer.getClient().addPiece(pieceIndex);
	}

	@Override
	public int getLength() {
		return 5;
	}
	
	@Override
	public void setReadDuration(int duration) {
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_HAVE;
	}
	
	@Override
	public String toString() {
		return "Have";
	}

}
