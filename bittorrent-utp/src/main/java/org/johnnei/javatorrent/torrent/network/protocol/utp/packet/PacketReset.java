package org.johnnei.javatorrent.torrent.network.protocol.utp.packet;

import org.johnnei.javatorrent.network.protocol.UtpSocket;
import org.johnnei.javatorrent.network.protocol.utp.ConnectionState;
import org.johnnei.javatorrent.torrent.network.Stream;

public class PacketReset extends Packet {
	
	public PacketReset() {
		
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
	}

	@Override
	public int getId() {
		return UtpProtocol.ST_RESET;
	}

	@Override
	public int getSize() {
		return 20;
	}
}
