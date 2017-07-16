package org.johnnei.javatorrent.internal.utp.protocol.packet;

/**
 * Represents a single packet on the uTP connection.
 */
public class UtpPacket {

	private final UtpHeader header;

	private final Payload payload;

	public UtpPacket(UtpHeader header, Payload payload) {
		assert header.getType() == payload.getType().getTypeField() : "Payload type doesn't confirm to the information in header.";
		this.header = header;
		this.payload = payload;
	}

	public UtpHeader getHeader() {
		return header;
	}

	public Payload getPayload() {
		return payload;
	}

	public boolean isSendOnce() {
		// FIXME: This should be false when the packet has been resent. (JBT-65)
		return true;
	}
}
