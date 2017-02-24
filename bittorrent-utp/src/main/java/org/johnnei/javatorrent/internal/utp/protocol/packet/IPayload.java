package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

public interface IPayload {

    /**
     * @return The type of the payload
     */
    PacketType getType();

    /**
     * @return The data which is associated with this payload.
     */
    byte[] getData();

}
