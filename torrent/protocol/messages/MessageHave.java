package torrent.protocol.messages;

import torrent.download.Torrent;
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
		if(peer.getTorrent().getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA)
			peer.getClient().havePiece(pieceIndex, true);
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
