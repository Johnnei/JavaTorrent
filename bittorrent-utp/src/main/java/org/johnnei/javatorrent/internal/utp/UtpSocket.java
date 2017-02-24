package org.johnnei.javatorrent.internal.utp;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

public class UtpSocket {

    private int connectionId;

    private int remoteConnectionId;

    private final AtomicInteger sequenceNumberCounter;

    private Queue<UtpPacket> receivedPackets;

    public UtpSocket() {
        sequenceNumberCounter = new AtomicInteger(0);
    }
}
