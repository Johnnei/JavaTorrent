package torrent.protocol.messages;

import torrent.download.peer.Peer;
import torrent.network.InStream;
import torrent.network.OutStream;
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
	public void write(OutStream outStream) {
		for (int i = 0; i < bitfield.length; i++) {
			outStream.writeByte(bitfield[i]);
		}
	}

	@Override
	public void read(InStream inStream) {
		bitfield = new byte[inStream.available()];
		for (int i = 0; i < bitfield.length; i++) {
			bitfield[i] = (byte) inStream.readByte();
		}
	}

	@Override
	public void process(Peer peer) {
		int pieceIndex = 0;
		for (int byteIndex = 0; byteIndex < bitfield.length; byteIndex++) {
			byte b = bitfield[byteIndex];
			for (int i = 0; i < 8; i++) {
				boolean isSet = ((b >> (7 - i)) & 0x1) != 0;
				
				if (isSet) {
					peer.havePiece(pieceIndex);
				}
				
				pieceIndex++;
			}
		}
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
