package torrent.protocol.messages;

import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;

public class MessageCancel implements IMessage {

	private int index;
	private int begin;
	private int offset;
	
	public MessageCancel() {
		
	}
	
	public MessageCancel(int index, int begin, int offset) {
		this.index = index;
		this.begin = begin;
		this.offset = offset;
	}

	@Override
	public void write(Stream outStream) {
		outStream.writeInt(index);
		outStream.writeInt(begin);
		outStream.writeInt(offset);
	}

	@Override
	public void read(Stream inStream) {
		index = inStream.readInt();
		begin = inStream.readInt();
		offset = inStream.readInt();
	}

	@Override
	public void process(Peer peer) {
		peer.getClient().removeJob(new Job(index, peer.getTorrent().getTorrentFiles().getBlockIndexByOffset(offset)));
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
	public String toString() {
		return "Cancel";
	}

}
