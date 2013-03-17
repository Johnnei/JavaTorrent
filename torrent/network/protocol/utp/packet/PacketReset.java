package torrent.network.protocol.utp.packet;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;
import torrent.network.protocol.utp.ConnectionState;

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
