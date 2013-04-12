package torrent.network.protocol.utp.packet;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;
import torrent.network.protocol.utp.ConnectionState;

public class PacketState extends Packet {
	
	public PacketState() {
	}
	
	public PacketState(int ackNumber) {
		this.acknowledgeNumber = ackNumber;
	}

	@Override
	protected void writePacket(Stream outStream) {
	}

	@Override
	protected void readPacket(Stream inStream) {
	}

	@Override
	public void processPacket(UtpSocket socket) {
		//System.out.println(socket.getPeerClient().getConnectionId() + "| ACKed " + acknowledgeNumber);
		if(acknowledgeNumber == 1 && socket.getConnectionState() == ConnectionState.CONNECTING) {
			socket.setConnectionState(ConnectionState.CONNECTED);
			socket.setUtpInputNumber(sequenceNumber);
		}
	}

	@Override
	public int getId() {
		return UtpProtocol.ST_STATE;
	}

	@Override
	public int getSize() {
		return 20;
	}
}
