package org.johnnei.javatorrent.bittorrent.protocol.messages;

import java.time.Duration;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class MessageBlock implements IMessage {

	private int index;
	private int offset;
	private byte[] data;

	private Duration readDuration;

	public MessageBlock() {
		/* Default constructor for reading */
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
		outStream.write(data);
	}

	@Override
	public void read(InStream inStream) {
		index = inStream.readInt();
		offset = inStream.readInt();
		data = inStream.readFully(inStream.available());
		readDuration = inStream.getReadDuration().orElse(null);
	}

	@Override
	public void process(Peer peer) {
		TorrentFileSet torrentFileSet = peer.getTorrent().getFileSet();

		peer.onReceivedBlock(torrentFileSet.getPiece(index), offset);
		if (data.length <= 0) {
			peer.addStrike(1);
			return;
		}

		peer.getTorrent().onReceivedBlock(torrentFileSet, index, offset, data);
		peer.getTorrent().getRequestLimiter().onReceivedBlock(peer, this);

		peer.addStrike(-1);
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
	public String toString() {
		return String.format("MessageBlock[index=%d, offset=%d, length=%d]", index, offset, data != null ? data.length : -1);
	}

	public Optional<Duration> getReadDuration() {
		return Optional.ofNullable(readDuration);
	}
}
