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
	
	private long readDuration;
	
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
		readDuration = System.currentTimeMillis();
		index = inStream.readInt();
		offset = inStream.readInt();
		data = inStream.readByteArray(inStream.available());
		readDuration = System.currentTimeMillis() - readDuration;
	}

	@Override
	public void process(Peer peer) {
		peer.getTorrent().collectPiece(index, offset, data);
		peer.getMyClient().removeJob(new Job(index, peer.getTorrent().getTorrentFiles().getBlockIndexByOffset(offset)));
		if(readDuration > 0) {
			peer.log("Retrieved block of " + data.length + " bytes in " + readDuration + " ms which comes down to " + (data.length / (readDuration / 1000)) + "B/s");
			peer.getMyClient().setMaxRequests((int)Math.ceil(data.length / (int)readDuration));
		} else {
			peer.log("Retrieved block of " + data.length + " bytes in " + readDuration + " ms");
		}
	}

	@Override
	public int getLength() {
		return 13 + data.length;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_PIECE;
	}
	
	@Override
	public String toString() {
		return "Block";
	}

}
