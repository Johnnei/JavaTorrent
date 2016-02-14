package org.johnnei.javatorrent.torrent.protocol.messages;

import java.time.Duration;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.download.peer.Job;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.peer.PeerDirection;
import org.johnnei.javatorrent.torrent.protocol.BitTorrent;

public class MessageBlock implements IMessage {

	private int index;
	private int offset;
	private byte[] data;

	private Duration readDuration;

	public MessageBlock() {
	}

	public MessageBlock(int index, int offset, byte[] data) {
		this.index = index;
		this.offset = offset;
		this.data = data;
	}

	@Override
	public void write(OutStream outStream) {
		outStream.writeInt(index);
		outStream.writeInt(offset);
		outStream.writeByte(data);
	}

	@Override
	public void read(InStream inStream) {
		index = inStream.readInt();
		offset = inStream.readInt();
		data = inStream.readFully(inStream.available());
	}

	@Override
	public void process(Peer peer) {
		peer.getTorrent().collectPiece(index, offset, data);
		peer.removeJob(new Job(index, peer.getTorrent().getFiles().getBlockIndexByOffset(offset)), PeerDirection.Download);
		if (data.length <= 0) {
			peer.addStrike(1);
			return;
		}

		peer.addStrike(-1);
		if (readDuration.minusSeconds(1).isNegative()) { // Set by extreme speed
			peer.setRequestLimit((int) Math.ceil(1000 / readDuration.toMillis()));
		} else if (peer.getRequestLimit() < 5) {
			// Set by trust
			peer.setRequestLimit(2 * (peer.getRequestLimit() + 1));
		}
	}

	@Override
	public int getLength() {
		return 9 + data.length;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_PIECE;
	}

	@Override
	public void setReadDuration(Duration duration) {
		readDuration = duration;
	}

	@Override
	public String toString() {
		return "Block";
	}

}
