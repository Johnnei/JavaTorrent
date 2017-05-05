package org.johnnei.javatorrent.internal.utp.protocol.packet;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

public class DataPayload implements Payload {

	private final byte[] data;

	public DataPayload(ByteBuffer data) {
		this.data = Objects.requireNonNull(data).array();
	}

	@Override
	public void onReceivedPayload(UtpSocket socket) {
		if (socket.getConnectionState() == ConnectionState.SYN_RECEIVED) {
			socket.setConnectionState(ConnectionState.CONNECTED);
		}

		// FIXME Handle processing of date.
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
