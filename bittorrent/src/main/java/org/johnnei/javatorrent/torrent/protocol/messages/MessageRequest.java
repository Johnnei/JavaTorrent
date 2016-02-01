package org.johnnei.javatorrent.torrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.download.peer.Job;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.peer.PeerDirection;
import org.johnnei.javatorrent.torrent.protocol.BitTorrent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MessageRequest implements IMessage {

	private static final Logger LOGGER = LoggerFactory.getLogger(MessageRequest.class);

	private int index;
	private int offset;
	private int length;

	public MessageRequest() {
		/* Empty constructor for receiving messages */
	}

	public MessageRequest(int index, int offset, int length) {
		this.index = index;
		this.offset = offset;
		this.length = length;
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
		if (peer.getTorrent().getFiles().hasPiece(index)) {
			peer.addJob(new Job(index, offset, length), PeerDirection.Upload);
		} else {
			LOGGER.error(String.format("Requested piece %d which I don't have", index));
			peer.getBitTorrentSocket().close();
		}
	}

	@Override
	public int getLength() {
		return 13;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_REQUEST;
	}

	@Override
	public void setReadDuration(int duration) {
	}

	@Override
	public String toString() {
		return "Request";
	}

}
