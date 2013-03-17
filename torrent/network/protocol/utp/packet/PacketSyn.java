package torrent.network.protocol.utp.packet;

import java.util.Random;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;
import torrent.network.protocol.utp.ConnectionState;

public class PacketSyn extends Packet {
	
	public PacketSyn() {
		
	}

	@Override
	protected void writePacket(Stream outStream) {
	}

	@Override
	protected void readPacket(Stream inStream) {
	}

	@Override
	public void processPacket(UtpSocket socket) {
		socket.getMyClient().setConnectionId(connectionId);
		socket.getPeerClient().setConnectionId(connectionId + 1);
		socket.setSequenceNumber(new Random().nextInt() & 0xFFFF);
		socket.sendPacket(new PacketSynResponse());
		socket.setConnectionState(ConnectionState.CONNECTED);
	}

	@Override
	public int getId() {
		return UtpProtocol.ST_SYN;
	}

	@Override
	public int getSize() {
		return 20;
	}
}
