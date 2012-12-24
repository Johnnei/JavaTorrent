package torrent.protocol.messages;

import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;

public class MessageBlock implements IMessage {

	private int index;
	private int offset;
	private byte[] data;
	
	public MessageBlock() {
	}
	
	public MessageBlock(int index, int offset, byte[] data) {
		this.index = index;
		this.offset = offset;
		this.data = data;
	}
	
	@Override
	public void write(Stream outStream) {
		outStream.writeInt(index);
		outStream.writeInt(offset);
		outStream.writeByte(data);
	}

	@Override
	public void read(Stream inStream) {
		index = inStream.readInt();
		offset = inStream.readInt();
		data = inStream.readByteArray(inStream.available());
	}

	@Override
	public void process(Peer peer) {
		peer.getTorrent().collectPiece(index, offset, data);
		peer.getMyClient().removeJob(new Job(index, peer.getTorrent().getTorrentFiles().getBlockIndexByOffset(offset)));
	}

	@Override
	public int getLength() {
		return 13 + data.length;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_PIECE;
	}

}
