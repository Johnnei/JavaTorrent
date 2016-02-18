package org.johnnei.javatorrent.bittorrent.protocol.messages;

import java.time.Duration;
import java.util.Arrays;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

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
			bitfield[i] = inStream.readByte();
		}
	}

	@Override
	public void process(Peer peer) {
		int pieceIndex = 0;
		for (byte b : bitfield) {
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
	public void setReadDuration(Duration duration) {
	}

	@Override
	public String toString() {
		return String.format("MessageBitfield[bitfield=%s]", Arrays.toString(bitfield));
	}

}
