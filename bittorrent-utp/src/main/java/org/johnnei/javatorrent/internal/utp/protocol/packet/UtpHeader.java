package org.johnnei.javatorrent.internal.utp.protocol.packet;

/**
 * Represents the header of an {@link UtpPacket}.
 */
public class UtpHeader {

	/**
	 * The currently implemented version.
	 */
	public static final byte IMPLEMENTED_VERSION = 1;

	private final byte type;

	private final byte version;

	private final byte extension;

	private final short connectionId;

	private final int windowSize;

	private final short sequenceNumber;

	private int timestamp;

	private int timestampDifference;

	private short acknowledgeNumber;

	private UtpHeader(UtpHeader.Builder builder) {
		type = builder.type;
		version = IMPLEMENTED_VERSION;
		extension = builder.extension;
		connectionId = builder.connectionId;
		timestamp = builder.timestamp;
		timestampDifference = builder.timestampDifference;
		windowSize = builder.windowSize;
		sequenceNumber = builder.sequenceNumber;
		acknowledgeNumber = builder.acknowledgeNumber;
	}

	/**
	 * Renews the packet for sending.
	 *
	 * @param acknowledgeNumber The new packet to acknowledge
	 * @param timestamp The time at which this was sent.
	 * @param timestampDifference The measured delay on the socket.
	 */
	public void renew(short acknowledgeNumber, int timestamp, int timestampDifference) {
		this.acknowledgeNumber = acknowledgeNumber;
		this.timestamp = timestamp;
		this.timestampDifference = timestampDifference;
	}

	public byte getType() {
		return type;
	}

	public byte getVersion() {
		return version;
	}

	public byte getExtension() {
		return extension;
	}

	public short getConnectionId() {
		return connectionId;
	}

	/**
	 * @return The sent time in microseconds.
	 */
	public int getTimestamp() {
		return timestamp;
	}

	public int getTimestampDifference() {
		return timestampDifference;
	}

	public int getWindowSize() {
		return windowSize;
	}

	public short getSequenceNumber() {
		return sequenceNumber;
	}

	public short getAcknowledgeNumber() {
		return acknowledgeNumber;
	}

	/**
	 * A builder to create headers in a readable manner.
	 */
	public static final class Builder {

		private byte type;

		private byte extension;

		private short connectionId;

		private int timestamp;

		private int timestampDifference;

		private int windowSize;

		private short sequenceNumber;

		private short acknowledgeNumber;

		public Builder setType(byte type) {
			this.type = type;
			return this;
		}

		public Builder setExtension(byte extension) {
			this.extension = extension;
			return this;
		}

		public Builder setConnectionId(short connectionId) {
			this.connectionId = connectionId;
			return this;
		}

		public Builder setTimestamp(int timestamp) {
			this.timestamp = timestamp;
			return this;
		}

		public Builder setTimestampDifference(int timestampDifference) {
			this.timestampDifference = timestampDifference;
			return this;
		}

		public Builder setWindowSize(int windowSize) {
			this.windowSize = windowSize;
			return this;
		}

		public Builder setSequenceNumber(short sequenceNumber) {
			this.sequenceNumber = sequenceNumber;
			return this;
		}

		public Builder setAcknowledgeNumber(short acknowledgeNumber) {
			this.acknowledgeNumber = acknowledgeNumber;
			return this;
		}

		public UtpHeader build() {
			return new UtpHeader(this);
		}
	}

}
