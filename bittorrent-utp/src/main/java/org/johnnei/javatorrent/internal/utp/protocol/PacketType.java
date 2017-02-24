package org.johnnei.javatorrent.internal.utp.protocol;

import java.util.function.Function;

import org.johnnei.javatorrent.internal.utp.protocol.packet.IPayload;

/**
 * The types of payload which can be contained in a {@link org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket}
 */
public enum PacketType {

    DATA(0, data -> null),
    FIN(1, data -> null),
    STATE(2, data -> null),
    RESET(3, data -> null),
    SYN(4, data -> null);

    private final byte typeField;

    private final Function<byte[], IPayload> payloadProducer;

    PacketType(int typeField, Function<byte[], IPayload> payloadProducer) {
        this.typeField = (byte) typeField;
        this.payloadProducer = payloadProducer;
    }

    /**
     * @return The byte value which represents this type.
     */
    public byte getTypeField() {
        return typeField;
    }

    /**
     * Factory method to create payload instances.
     * @param data The read data from the stream
     * @return The newly created IPayload instance.
     */
    public IPayload createPayload(byte[] data) {
        return payloadProducer.apply(data);
    }

    /**
     * @param typeField The {@link #typeField} value of the enum.
     * @return The associated enum value.
     * @throws IllegalArgumentException When the typeField does not exist.
     */
    public static PacketType getByType(int typeField) {
        for (PacketType type : values()) {
            if (type.typeField == typeField) {
                return type;
            }
        }

        throw new IllegalArgumentException(String.format("No type is mapped for value %d", typeField));
    }
}
