package torrent.network.protocol.utp.packet;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;

public class PacketFin extends Packet {
	
	private byte[] data;
	
	public PacketFin() {
		data = new byte[0];
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
		return UtpProtocol.ST_DATA;
	}

	@Override
	public int getSize() {
		return 20 + data.length;
	}
}
