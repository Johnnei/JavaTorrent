package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

public class SynPayload extends DatalessPayload {

	@Override
	public void onReceivedPayload(UtpSocket socket) {
		socket.setConnectionState(ConnectionState.SYN_RECEIVED);
		// The sending of the ST_STATE on the ST_SYN is designed to be handled by normal ACK handling system.
	}

	@Override
	public PacketType getType() {
		return PacketType.SYN;
	}
}
