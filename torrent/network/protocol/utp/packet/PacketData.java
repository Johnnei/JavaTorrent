package torrent.network.protocol.utp.packet;

import java.io.IOException;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;
import torrent.network.protocol.utp.UtpInputStream;

public class PacketData extends Packet {
	
	private byte[] data;
	
	public PacketData() {
		data = new byte[0];
	}
	
	public PacketData(byte[] data) {
		this.data = data;
	}

	@Override
	protected void writePacket(Stream outStream) {
		outStream.writeByte(data);
	}

	@Override
	protected void readPacket(Stream inStream) {
		data = inStream.readByteArray(inStream.available());
	}

	@Override
	public void processPacket(UtpSocket socket) {
		try {
			UtpInputStream inputStream = (UtpInputStream)socket.getInputStream();
			inputStream.receiveData(this);
		} catch (IOException e) {
			//This exception won't be thrown
		}
	}

	@Override
	public int getId() {
		return UtpProtocol.ST_DATA;
	}

	@Override
	public int getSize() {
		return 20 + data.length;
	}
	
	public byte[] getData() {
		return data;
	}
}
