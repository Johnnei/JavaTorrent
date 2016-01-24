package torrent.network.protocol.utp.packet;

import java.io.IOException;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;
import torrent.network.protocol.utp.UtpInputStream;

public class PacketData extends Packet {
	
	private byte[] data;
	
	public PacketData() {
		super();
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
		System.err.println(socket.getMyClient().getConnectionId() + "| WE GOT DATA!");
		try {
			UtpInputStream inputStream = (UtpInputStream)socket.getInputStream();
			inputStream.receiveData(this);
		} catch (IOException e) {
			//This exception won't be thrown
		}
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
