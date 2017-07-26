package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.johnnei.javatorrent.internal.utp.stream.PacketWriter;

/**
 * Represents a single packet on the uTP connection.
 */
public class UtpPacket {

	private final UtpHeader header;

	private final Payload payload;

	private int timesSent;

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
		return timesSent == 1;
	}

	public void incrementSentCount() {
		timesSent++;
	}

	public int getSize() {
		return PacketWriter.OVERHEAD_IN_BYTES + payload.getData().length;
	}
}
