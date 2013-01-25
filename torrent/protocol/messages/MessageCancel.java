package torrent.protocol.messages;

import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;

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
	public void write(Stream outStream) {
		outStream.writeInt(index);
		outStream.writeInt(offset);
		outStream.writeInt(length);
	}

	@Override
	public void read(Stream inStream) {
		index = inStream.readInt();
		offset = inStream.readInt();
		length = inStream.readInt();
	}

	@Override
	public void process(Peer peer) {
		peer.getClient().removeJob(new Job(index, offset));
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
	public void setReadDuration(int duration) {
	}

	@Override
	public String toString() {
		return "Cancel";
	}

}
