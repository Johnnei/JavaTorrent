package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

public class ResetPayload extends DatalessPayload {

	@Override
	public void onReceivedPayload(UtpHeader header, UtpSocket socket) {
		socket.setConnectionState(ConnectionState.RESET);
	}

	@Override
	public PacketType getType() {
		return PacketType.RESET;
	}
}
