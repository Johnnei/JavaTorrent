package org.johnnei.javatorrent.protocol.messages.ut_metadata;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.protocol.UTMetadata;
import org.johnnei.javatorrent.torrent.peer.Job;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

public class MessageData extends Message {

	private byte[] data;

	public MessageData() {

	}

	public MessageData(int piece, byte[] data) {
		super(piece);
		this.data = data;
	}

	@Override
	public void write(OutStream outStream) {
		super.write(outStream);
		outStream.writeByte(data);
	}

	@Override
	public void read(InStream inStream) {
		super.read(inStream);
		data = inStream.readFully(inStream.available());
	}

	@Override
	public void process(Peer peer) {
		int blockIndex = (int) dictionary.get("piece");
		peer.getTorrent().collectPiece(0, blockIndex * peer.getTorrent().getFiles().getBlockSize(), data);
		peer.removeJob(new Job(0, blockIndex), PeerDirection.Download);
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
