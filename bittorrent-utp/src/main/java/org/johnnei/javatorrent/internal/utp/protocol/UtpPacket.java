package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;
import java.time.Clock;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.payload.IPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.UtpPayloadFactory;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

/**
 * The header section of an UTP packet.
 */
public class UtpPacket {

	/**
	 * The clock used to measure the current timestamp in microseconds.
	 */
	private Clock clock = Clock.systemDefaultZone();

	/**
	 * The payload type of the packet.
	 */
	private byte type;

	/**
	 * The version of the packet.
	 */
	private byte version;

	/**
	 * The type of the first extension in the header.
	 */
	private byte extension;

	/**
	 * The connection id.
	 */
	private short connectionId;

	/**
	 * The timestamp at which this packet was send.
	 */
	private int timestampMicroseconds;

	/**
	 * The measured difference with the other end.
	 */
	private int timestampDifferenceMicroseconds;

	/**
	 * The amount of bytes which have not yet been acked.
	 */
	private int windowSize;

	/**
	 * The sequence number of the packet.
	 */
	private short sequenceNumber;

	/**
	 * The sequence number of the last received packet.
	 */
	private short acknowledgeNumber;

	/**
	 * The payload of the packet
	 */
	private IPayload payload;

	public UtpPacket() {
		// Constructor for reading
	}

	public UtpPacket(UtpSocketImpl socket, IPayload payload) {
		this.version = UtpProtocol.VERSION;
		this.type = payload.getType();
		this.payload = payload;

		if (type == UtpProtocol.ST_STATE && socket.getConnectionState() != ConnectionState.CONNECTING) {
			this.sequenceNumber = socket.getSequenceNumber();
		} else {
			this.sequenceNumber = socket.nextSequenceNumber();
		}

		if (type == UtpProtocol.ST_SYN) {
			connectionId = socket.getReceivingConnectionId();
		} else {
			connectionId = socket.getSendingConnectionId();
		}
	}

	public void read(InStream inStream, UtpPayloadFactory payloadFactory) {
		int typeAndVersion = inStream.readByte();
		version = (byte) (typeAndVersion & 0x0F);

		if (version != UtpProtocol.VERSION) {
			// We probably can't process this, so don't.
			return;
		}

		payload = payloadFactory.createPayloadFromType(typeAndVersion >>> 4);
		extension = inStream.readByte();
		connectionId = inStream.readShort();
		timestampMicroseconds = inStream.readInt();
		timestampDifferenceMicroseconds = inStream.readInt();
		windowSize = inStream.readInt();
		sequenceNumber = inStream.readShort();
		acknowledgeNumber = inStream.readShort();
		payload.read(inStream);
	}

	public void write(UtpSocketImpl socket, OutStream outStream) {
		int typeAndVersion = (type << 4) | (version & 0xF);
		outStream.writeByte(typeAndVersion);
		// We don't support extension yet.
		outStream.writeByte(0);
		outStream.writeShort(connectionId);
		outStream.writeInt(clock.instant().getNano() / 1000);
		outStream.writeInt(socket.getMeasuredDelay());
		outStream.writeInt(socket.getWindowSize());
		outStream.writeShort(sequenceNumber);
		outStream.writeShort(socket.getAcknowledgeNumber());
		payload.write(outStream);
	}

	/**
	 * Processes the payload information.
	 * @param socket The socket which received the packet.
	 */
	public void processPayload(UtpSocketImpl socket) throws IOException {
		payload.process(this, socket);
	}

	public short getConnectionId() {
		return connectionId;
	}

	public short getAcknowledgeNumber() {
		return acknowledgeNumber;
	}

	public short getSequenceNumber() {
		return sequenceNumber;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public int getTimestampMicroseconds() {
		return timestampMicroseconds;
	}

	public int getTimestampDifferenceMicroseconds() {
		return timestampDifferenceMicroseconds;
	}

	public boolean hasExtensions() {
		return extension != 0;
	}

	public byte getVersion() {
		return version;
	}

	public int getPacketSize() {
		return 20 + payload.getSize();
	}

	@Override
	public String toString() {
		return String.format("UtpPacket[payload=%s, seq=%s, ack=%s]", payload, Short.toUnsignedInt(sequenceNumber), Short.toUnsignedInt(acknowledgeNumber));
	}
}
