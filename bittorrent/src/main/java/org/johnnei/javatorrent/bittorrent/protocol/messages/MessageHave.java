package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class MessageHave implements IMessage {

	private int pieceIndex;

	public MessageHave() {
		/* Default constructor for reading */
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
	public int getId() {
		return BitTorrent.MESSAGE_HAVE;
	}

	@Override
	public String toString() {
		return String.format("MessageHave[piece=%s]", pieceIndex);
	}

}
