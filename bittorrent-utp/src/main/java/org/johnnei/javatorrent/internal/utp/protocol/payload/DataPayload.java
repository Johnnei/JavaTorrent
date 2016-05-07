package org.johnnei.javatorrent.internal.utp.protocol.payload;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

/**
 * Data payload which provides the actual application layer data.
 */
public class DataPayload implements IPayload {

	private byte[] data;

	public DataPayload(byte[] data) {
		this.data = data;
	}

	public DataPayload() {
		// Constructor for the purpose of receiving packets
		this.data = new byte[0];
	}

	@Override
	public byte getType() {
		return UtpProtocol.ST_DATA;
	}

	@Override
	public void read(InStream inStream) {
		data = inStream.readFully(inStream.available());
	}

	@Override
	public void write(OutStream outStream) {
		outStream.write(data);
	}

	@Override
	public void process(UtpPacket packet, UtpSocketImpl socket) throws IOException {
		socket.getInputStream().addToBuffer(packet.getSequenceNumber(), this);
	}

	public byte[] getData() {
		return data;
	}

	@Override
	public int getSize() {
		return data.length;
	}

	@Override
	public String toString() {
		return String.format("DataPayload[length=%s]", data.length);
	}
}
