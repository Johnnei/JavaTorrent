package torrent.protocol.messages;

import torrent.download.peer.Job;
import torrent.download.peer.PeerDirection;
import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;

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
		if (peer.getTorrent().getFiles().getBitfield().hasPiece(index)) {
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
