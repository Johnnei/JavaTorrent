package torrent.network.protocol.utp.packet;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;

public class PacketState extends Packet {
	
	public PacketState() {
	}

	@Override
	protected void writePacket(Stream outStream) {
	}

	@Override
	protected void readPacket(Stream inStream) {
	}

	@Override
	public void processPacket(UtpSocket socket) {
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
