package org.johnnei.javatorrent.internal.utp.protocol.packet;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

public class DataPayload implements Payload {

	private final byte[] data;

	public DataPayload(ByteBuffer data) {
		Objects.requireNonNull(data);

		this.data = new byte[data.remaining()];
		System.arraycopy(data.array(), data.arrayOffset() + data.position(), this.data, 0, this.data.length);
	}

	@Override
	public void onReceivedPayload(UtpHeader header, UtpSocket socket) {
		if (socket.getConnectionState() == ConnectionState.SYN_RECEIVED) {
			socket.setConnectionState(ConnectionState.CONNECTED);
		}

		socket.submitData(header.getSequenceNumber(), data);
	}

	@Override
	public byte[] getData() {
		return data;
	}

	@Override
	public PacketType getType() {
		return PacketType.DATA;
	}
}
