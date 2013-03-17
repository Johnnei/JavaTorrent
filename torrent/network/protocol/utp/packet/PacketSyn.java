package torrent.network.protocol.utp.packet;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;

public class PacketSyn extends Packet {
	
	public PacketSyn() {
		
	}

	@Override
	protected void writePacket(Stream outStream) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void readPacket(Stream inStream) {
		// TODO Auto-generated method stub

	}

	@Override
	public void processPacket(UtpSocket socket) {
		// TODO Auto-generated method stub

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
