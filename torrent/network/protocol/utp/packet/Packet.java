package torrent.network.protocol.utp.packet;

import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;

public abstract class Packet implements Comparable<Packet> {
	
	public static final int VERSION = 1;
	protected UtpSocket socket;
	
	protected short connectionId;
	protected long sendTimestamp;
	protected long delay;
	protected long windowSize;
	protected int sequenceNumber;
	protected int acknowledgeNumber;
	
	public Packet() {
		sequenceNumber = -1;
	}
	
	public Packet(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}
	
	public void setSocket(UtpSocket socket) {
		this.socket = socket;
	}
	
	/**
	 * Writes the message to the outputstream
	 * @param outStream
	 */
	public void write(Stream outStream) {
		//Write Header
		outStream.writeByte(getId() << 4 | VERSION);
		outStream.writeByte(0); //No Extension Support
		outStream.writeShort(socket.getMyClient().getConnectionId());
		sendTimestamp = UtpProtocol.getMicrotime(); //Set timestamps so we can resend lost packets
		outStream.writeInt(sendTimestamp); //"Micro"second timestamp
		outStream.writeInt(socket.getPeerClient().getDelay());
		outStream.writeInt(socket.getMyClient().getWindowSize());
		outStream.writeShort(getSendSequenceNumber());
		if(acknowledgeNumber == 0)
			outStream.writeShort(socket.getAcknowledgeNumber());
		else
			outStream.writeShort(acknowledgeNumber);
		//Write Extra Data if needed
		writePacket(outStream);
	}
	
	/**
	 * Reads the message from the inputstream
	 * @param inStream
	 */
	public void read(Stream inStream) {
		inStream.readByte(); //We already checked this in the UdpMultiplexer
		int extension = inStream.readByte();
		connectionId = (short)inStream.readShort();
		sendTimestamp = inStream.readInt() & 0xFFFFFFFFL;
		delay = inStream.readInt() & 0xFFFFFFFFL;
		windowSize = inStream.readInt() & 0xFFFFFFFFL;
		sequenceNumber = inStream.readShort() & 0xFFFF;
		acknowledgeNumber = inStream.readShort() & 0xFFFF;
		while(extension != 0) {
			int newExtension = inStream.readByte();
			int length = inStream.readByte();
			//TODO Process Extensions
			inStream.moveBack(-length);
			extension = newExtension;
		}
		readPacket(inStream);
	}
	
	/**
	 * Writes the message to the output stream
	 * 
	 * @param outStream
	 */
	protected abstract void writePacket(Stream outStream);

	/***
	 * Read a message from the inputStream
	 * 
	 * @param inStream
	 */
	protected abstract void readPacket(Stream inStream);
	
	/**
	 * Gets the sequence number for this packet<br/>
	 * In PacketData this will be overriden to increase the number
	 * @return
	 */
	protected int getSendSequenceNumber() {
		if(sequenceNumber != -1)
			return sequenceNumber;
		else {
			sequenceNumber = socket.getSequenceNumber();
			return sequenceNumber;
		}
	}
	
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	
	public long getSendTime() {
		return sendTimestamp;
	}

	public void process(UtpSocket socket) {
		//Process Header
		socket.getPeerClient().setWindowSize(windowSize);
		socket.getPeerClient().setDelay(UtpProtocol.getMicrotime() - sendTimestamp, false);
		socket.getMyClient().setDelay(delay);
		//System.out.println("Our Delay: " + delay + ", Translated: " + socket.getMyClient().getDelay() + "us, they send at: " + sendTimestamp);
		socket.setAcknowledgeNumber(sequenceNumber, needAcknowledgement());
		socket.acknowledgedPacket(acknowledgeNumber);
		//Process Packet
		processPacket(socket);
	}
	
	public abstract void processPacket(UtpSocket socket);
	
	public abstract int getId();
	
	public abstract int getSize();
	
	public boolean needAcknowledgement() {
		return false;
	}
	
	@Override
	public int compareTo(Packet otherPacket) {
		return sequenceNumber - otherPacket.sequenceNumber;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o instanceof Packet) {
			Packet p = (Packet)o;
			return p.sequenceNumber == sequenceNumber;
		} else {
			return false;
		}
	}

	public short getConnectionId() {
		return connectionId;
	}
}
