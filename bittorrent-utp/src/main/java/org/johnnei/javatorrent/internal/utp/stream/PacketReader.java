package org.johnnei.javatorrent.internal.utp.stream;

import java.nio.ByteBuffer;

import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.packet.Payload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

public class PacketReader {

	public UtpPacket read(ByteBuffer inStream) {

		int typeAndVersion = inStream.get();

		UtpHeader header = new UtpHeader.Builder()
			.setType((byte) (typeAndVersion >>> 4))
			.setExtension(inStream.get())
			.setConnectionId(inStream.getShort())
			.setTimestamp(inStream.getInt())
			.setTimestampDifference(inStream.getInt())
			.setWindowSize(inStream.getInt())
			.setSequenceNumber(inStream.getShort())
			.setAcknowledgeNumber(inStream.getShort())
			.build();

		Payload payload = PacketType.getByType(header.getType())
			.createPayload(inStream);

		return new UtpPacket(header, payload);
	}
}
