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
		for(int i = 0; i < bitfield.length; i++) {
			outStream.writeByte(bitfield[i]);
		}
	}

	@Override
	public void read(Stream inStream) {
		bitfield = new byte[inStream.available()];
		for(int i = 0; i < bitfield.length; i++) {
			bitfield[i] = (byte)inStream.readByte();
		}
	}

	@Override
	public void process(Peer peer) {
		int pieceIndex = 0;
		for(int i = 0; i < bitfield.length; i++) {
			int b = bitfield[i];
			for (int bit = 0; bit < 8; bit++) {
				if (((b >> bit) & 1) == 1) {
					peer.getClient().addPiece(pieceIndex);
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
