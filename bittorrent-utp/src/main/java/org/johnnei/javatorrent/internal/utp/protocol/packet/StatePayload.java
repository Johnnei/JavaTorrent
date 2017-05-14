package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

public class StatePayload extends DatalessPayload {

	@Override
	public void onReceivedPayload(UtpSocket socket) {
		// The state packet is only used to send out updated states in the UtpHeader.
		if (socket.getConnectionState() == ConnectionState.SYN_SENT) {
			socket.setConnectionState(ConnectionState.CONNECTED);
		}
	}

	@Override
	public PacketType getType() {
		return PacketType.STATE;
	}
}
