package torrent.network.protocol.utp.packet;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;
import torrent.network.protocol.utp.ConnectionState;

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
