package org.johnnei.javatorrent.bittorrent.protocol.messages;

import java.time.Duration;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

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
	public void setReadDuration(Duration duration) {
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
