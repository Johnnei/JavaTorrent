package torrent.protocol.messages.ut_metadata;

import torrent.download.peer.Job;
import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.UTMetadata;

public class MessageData extends Message {

	private byte[] data;

	public MessageData() {

	}

	public MessageData(int piece, byte[] data) {
		super(piece);
		this.data = data;
	}

	@Override
	public void write(Stream outStream) {
		super.write(outStream);
		outStream.writeByte(data);
	}

	@Override
	public void read(Stream inStream) {
		super.read(inStream);
		data = inStream.readByteArray(inStream.available());
	}

	@Override
	public void process(Peer peer) {
		int blockIndex = (int) dictionary.get("piece");
		peer.getTorrent().collectPiece(0, blockIndex * peer.getTorrent().getFiles().getBlockSize(), data);
		peer.getMyClient().removeJob(new Job(0, blockIndex));
	}

	@Override
	public int getLength() {
		return data.length + bencodedData.length();
	}

	@Override
	public int getId() {
		return UTMetadata.DATA;
	}

	@Override
	public String toString() {
		return "UT_Metadata Data";
	}

}
