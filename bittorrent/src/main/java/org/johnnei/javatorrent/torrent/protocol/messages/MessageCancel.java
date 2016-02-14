package org.johnnei.javatorrent.torrent.protocol.messages;

import java.time.Duration;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.download.peer.Job;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.peer.PeerDirection;
import org.johnnei.javatorrent.torrent.protocol.BitTorrent;

public class MessageCancel implements IMessage {

	private int index;
	private int offset;
	private int length;

	public MessageCancel() {

	}

	public MessageCancel(int index, int begin, int offset) {
		this.index = index;
		this.offset = begin;
		this.length = offset;
	}

	@Override
	public void write(OutStream outStream) {
		outStream.writeInt(index);
		outStream.writeInt(offset);
		outStream.writeInt(length);
	}

	@Override
	public void read(InStream inStream) {
		index = inStream.readInt();
		offset = inStream.readInt();
		length = inStream.readInt();
	}

	@Override
	public void process(Peer peer) {
		peer.removeJob(new Job(index, offset), PeerDirection.Upload);
	}

	@Override
	public int getLength() {
		return 13;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_CANCEL;
	}

	@Override
	public void setReadDuration(Duration duration) {
	}

	@Override
	public String toString() {
		return "Cancel";
	}

}
