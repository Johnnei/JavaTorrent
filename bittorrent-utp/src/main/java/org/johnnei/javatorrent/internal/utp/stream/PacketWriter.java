package org.johnnei.javatorrent.internal.utp.stream;

import java.nio.ByteBuffer;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

public class PacketWriter {

	/**
	 * The required amount of bytes in a packet which are used to coordinate the protocol.
	 */
	public static final int OVERHEAD_IN_BYTES = 20;

	public ByteBuffer write(UtpPacket packet) {
		ByteBuffer buffer = ByteBuffer.allocate(OVERHEAD_IN_BYTES + packet.getPayload().getData().length);

		byte typeAndVersion = (byte) ((packet.getHeader().getType() << 4) | (packet.getHeader().getVersion() & 0xF));
		buffer.put(typeAndVersion);
		// We don't support extension yet.
		buffer.put((byte) 0);
		buffer.putShort(packet.getHeader().getConnectionId());
		buffer.putInt(packet.getHeader().getTimestamp());
		buffer.putInt(packet.getHeader().getTimestampDifference());
		buffer.putInt(packet.getHeader().getWindowSize());
		buffer.putShort(packet.getHeader().getSequenceNumber());
		buffer.putShort(packet.getHeader().getAcknowledgeNumber());
		buffer.put(packet.getPayload().getData());

		buffer.flip();
		return buffer;
	}
}
