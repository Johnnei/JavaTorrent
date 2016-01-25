package torrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.protocol.IMessage;

import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.download.peer.PeerDirection;
import torrent.protocol.BitTorrent;

public class MessageRequest implements IMessage {

	private int index;
	private int offset;
	private int length;

	public MessageRequest() {

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
			peer.getLogger().severe(String.format("Requested piece %d which I don't have", index));
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
