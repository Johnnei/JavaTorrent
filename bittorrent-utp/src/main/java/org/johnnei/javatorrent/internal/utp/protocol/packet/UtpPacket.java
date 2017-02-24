package org.johnnei.javatorrent.internal.utp.protocol.packet;

/**
 * Represents a single packet on the uTP connection.
 */
public class UtpPacket {

    private final UtpHeader header;

    private final IPayload payload;

    public UtpPacket(UtpHeader header, IPayload payload) {
        this.header = header;
        this.payload = payload;
    }

    public UtpHeader getHeader() {
        return header;
    }

    public IPayload getPayload() {
        return payload;
    }
}
