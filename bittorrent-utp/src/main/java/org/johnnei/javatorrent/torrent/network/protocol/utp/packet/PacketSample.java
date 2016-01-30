package org.johnnei.javatorrent.torrent.network.protocol.utp.packet;

import org.johnnei.javatorrent.network.protocol.UtpSocket;
import org.johnnei.javatorrent.torrent.network.Stream;

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
