package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utp.protocol.payload.IPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.UtpPayloadFactory;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

/**
 * The header section of an UTP packet.
 */
public class UtpPacket {

	private PrecisionTimer timer = new PrecisionTimer();

	/**
	 * The time at which this packet was send.
	 */
	private int sentTime;

	/**
	 * The amount of times this packet was sent.
	 */
	private int timesSent;

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

	/**
	 * Creates a new UtpPacket which will be send.
	 * @param socket The socket on which this packet will be send.
	 * @param payload The payload which will be send with this packet.
	 */
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

	/**
	 * Reads the packet including the payload.
	 * @param inStream The stream from which the packet must be read.
	 * @param payloadFactory The payload factory to read the payload data.
	 */
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

	/**
	 * Writes the packet including the payload.
	 * @param socket The socket for which this packet will be written.
	 * @param outStream The stream on which the packet will be written.
	 */
	public void write(UtpSocketImpl socket, OutStream outStream) {
		int typeAndVersion = (type << 4) | (version & 0xF);
		outStream.writeByte(typeAndVersion);
		// We don't support extension yet.
		outStream.writeByte(0);
		outStream.writeShort(connectionId);
		outStream.writeInt(timer.getCurrentMicros());
		outStream.writeInt(socket.getMeasuredDelay());
		outStream.writeInt(socket.getWindowSize());
		outStream.writeShort(sequenceNumber);
		// Assign it so the toString is usefull during debugging.
		acknowledgeNumber = socket.getAcknowledgeNumber();
		outStream.writeShort(acknowledgeNumber);
		payload.write(outStream);
	}

	/**
	 * Updates the time at which this packet was sent and increments the amount of times it is send.
	 */
	public void updateSentTime() {
		sentTime = timer.getCurrentMicros();
		timesSent++;
	}

	/**
	 * Processes the payload information.
	 * @param socket The socket which received the packet.
	 */
	public void processPayload(UtpSocketImpl socket) throws IOException {
		payload.process(this, socket);
	}

	/**
	 * @return The connection ID for this packet.
	 */
	public short getConnectionId() {
		return connectionId;
	}

	/**
	 * @return The last packet which was received.
	 */
	public short getAcknowledgeNumber() {
		return acknowledgeNumber;
	}

	/**
	 * @return The sequence number of this packet.
	 */
	public short getSequenceNumber() {
		return sequenceNumber;
	}

	/**
	 * @return The advertised window size.
	 */
	public int getWindowSize() {
		return windowSize;
	}

	/**
	 * @return The timestamp at which this packet was sent.
	 */
	public int getTimestampMicroseconds() {
		return timestampMicroseconds;
	}

	/**
	 * @return The measured difference between packet timestamp and local timestamp.
	 */
	public int getTimestampDifferenceMicroseconds() {
		return timestampDifferenceMicroseconds;
	}

	/**
	 * @return <code>true</code> when the extension indication field is non-zero.
	 */
	public boolean hasExtensions() {
		return extension != 0;
	}

	/**
	 * @return The protocol version of this packet.
	 */
	public byte getVersion() {
		return version;
	}

	/**
	 * @return The size of the packet including the payload size.
	 */
	public int getPacketSize() {
		return 20 + payload.getSize();
	}

	/**
	 * @return The time at which this packet was sent.
	 */
	public int getSentTime() {
		return sentTime;
	}

	/**
	 * @return The amount of times this packet was sent.
	 */
	public int getTimesSent() {
		return timesSent;
	}

	@Override
	public String toString() {
		return String.format("UtpPacket[payload=%s, seq=%s, ack=%s]", payload, Short.toUnsignedInt(sequenceNumber), Short.toUnsignedInt(acknowledgeNumber));
	}
}
