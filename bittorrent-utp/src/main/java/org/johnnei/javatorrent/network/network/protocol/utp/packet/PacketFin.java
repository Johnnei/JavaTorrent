package org.johnnei.javatorrent.network.network.protocol.utp.packet;

import org.johnnei.javatorrent.network.protocol.UtpSocket;
import org.johnnei.javatorrent.network.protocol.utp.ConnectionState;
import org.johnnei.javatorrent.network.Stream;

public class PacketFin extends Packet {
	
	public PacketFin() {
	}

	@Override
	protected void writePacket(Stream outStream) {
	}

	@Override
	protected void readPacket(Stream inStream) {
	}

	@Override
	public void processPacket(UtpSocket socket) {
		socket.setConnectionState(ConnectionState.DISCONNECTING);
		socket.setFinalPacket(sequenceNumber);
	}
	
	@Override
	public boolean needAcknowledgement() {
		return true;
	}

	@Override
	public int getId() {
		return UtpProtocol.ST_FIN;
	}

	@Override
	public int getSize() {
		return 20;
	}
}
