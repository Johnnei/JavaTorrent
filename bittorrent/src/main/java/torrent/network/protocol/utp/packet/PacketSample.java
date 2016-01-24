package torrent.network.protocol.utp.packet;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;

/**
 * A sample packet which is used for comparing packets
 * @author Johnnei
 *
 */
public class PacketSample extends Packet {
	
	public PacketSample(int sequenceNumber) {
		super(sequenceNumber);
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
		return -1;
	}

	@Override
	public int getSize() {
		return 0;
	}

}
