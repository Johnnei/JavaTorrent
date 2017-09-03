package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.johnnei.javatorrent.internal.utp.UtpSocket;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

public class FinPayload extends DatalessPayload {

	@Override
	public void onReceivedPayload(UtpHeader header, UtpSocket socket) {
		socket.shutdownInputStream(header.getSequenceNumber());
	}

	@Override
	public PacketType getType() {
		return PacketType.FIN;
	}

}
