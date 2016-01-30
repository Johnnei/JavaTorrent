package org.johnnei.javatorrent.torrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.protocol.BitTorrent;

public class MessageHave implements IMessage {

	private int pieceIndex;

	public MessageHave() {
	}

	public MessageHave(int pieceIndex) {
		this.pieceIndex = pieceIndex;
	}

	@Override
	public void write(OutStream outStream) {
		outStream.writeInt(pieceIndex);
	}

	@Override
	public void read(InStream inStream) {
		pieceIndex = inStream.readInt();
	}

	@Override
	public void process(Peer peer) {
		peer.havePiece(pieceIndex);
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
