package torrent.protocol.messages;

import torrent.download.peer.Peer;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;

public class MessageBitfield implements IMessage {

	private byte[] bitfield;

	public MessageBitfield() {
		bitfield = new byte[0];
	}

	public MessageBitfield(byte[] bitfield) {
		this.bitfield = bitfield;
	}

	@Override
	public void write(Stream outStream) {
		for (int i = 0; i < bitfield.length; i++) {
			outStream.writeByte(bitfield[i]);
		}
	}

	@Override
	public void read(Stream inStream) {
		bitfield = new byte[inStream.available()];
		for (int i = 0; i < bitfield.length; i++) {
			bitfield[i] = (byte) inStream.readByte();
		}
	}

	@Override
	public void process(Peer peer) {
		peer.getClient().getBitfield().setBitfield(bitfield);
	}

	@Override
	public int getLength() {
		return 1 + bitfield.length;
	}

	@Override
	public int getId() {
		return BitTorrent.MESSAGE_BITFIELD;
	}

	@Override
	public void setReadDuration(int duration) {
	}

	@Override
	public String toString() {
		return "Bitfield";
	}

}
