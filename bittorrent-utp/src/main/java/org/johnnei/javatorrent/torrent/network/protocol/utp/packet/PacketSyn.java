package org.johnnei.javatorrent.torrent.network.protocol.utp.packet;

import java.util.Random;

import org.johnnei.javatorrent.network.protocol.UtpSocket;
import org.johnnei.javatorrent.network.protocol.utp.ConnectionState;
import org.johnnei.javatorrent.torrent.network.Stream;

public class PacketSyn extends Packet {
	
	public PacketSyn() {
		
	}

	@Override
	protected void writePacket(Stream outStream) {
		outStream.skipWrite(-18); //Skip back to Connection Id
		outStream.writeShort(socket.getPeerClient().getConnectionId());
		outStream.skipWrite(16); //Skip forward to end
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
	protected int getSendSequenceNumber() {
		if(sequenceNumber != -1)
			return sequenceNumber;
		else {
			sequenceNumber = socket.getNextSequenceNumber();
			return sequenceNumber;
		}
	}
	
	@Override
	public boolean needAcknowledgement() {
		return true;
	}

	@Override
	public int getSize() {
		return 20;
	}
}
