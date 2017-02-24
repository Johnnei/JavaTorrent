package org.johnnei.javatorrent.internal.utp.stream;

import java.io.IOException;

import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.packet.IPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;
import org.johnnei.javatorrent.network.InStream;

public class PacketReader {

    public UtpPacket read(InStream inStream) throws IOException {

        int typeAndVersion = inStream.readByte();

        UtpHeader header = new UtpHeader.Builder()
        .setType((byte) (typeAndVersion >>> 4))
        .setExtension(inStream.readByte())
        .setConnectionId(inStream.readShort())
        .setTimestamp(inStream.readInt())
        .setTimestampDifference(inStream.readInt())
        .setWindowSize(inStream.readInt())
        .setSequenceNumber(inStream.readShort())
        .setAcknowledgeNumber(inStream.readShort())
            .build();

        IPayload payload = PacketType.getByType(header.getType())
            .createPayload(inStream.readFully(inStream.available()));

        return new UtpPacket(header, payload);
    }
}
