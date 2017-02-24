package org.johnnei.javatorrent.internal.utp.stream;

import java.io.IOException;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;
import org.johnnei.javatorrent.network.OutStream;

public class PacketWriter {

    public void write(OutStream outStream, UtpPacket packet) throws IOException {
        int typeAndVersion = (packet.getHeader().getType() << 4) | (packet.getHeader().getVersion() & 0xF);
        outStream.writeByte(typeAndVersion);
        // We don't support extension yet.
        outStream.writeByte(0);
        outStream.writeShort(packet.getHeader().getConnectionId());
        outStream.writeInt(packet.getHeader().getTimestamp());
        outStream.writeInt(packet.getHeader().getTimestampDifference());
        outStream.writeInt(packet.getHeader().getWindowSize());
        outStream.writeShort(packet.getHeader().getSequenceNumber());
        outStream.writeShort(packet.getHeader().getAcknowledgeNumber());
        outStream.write(packet.getPayload().getData());
    }
}
