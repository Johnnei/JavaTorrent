package org.johnnei.javatorrent.internal.utp;

import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.DatagramChannel;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

public class UtpSocket implements Closeable {

	private int sendConnectionId;

	private int receiveConnectionId;

	private final AtomicInteger sequenceNumberCounter;

	private final DatagramChannel channel;

	private Queue<UtpPacket> receivedPackets;

	public UtpSocket(DatagramChannel channel) {
		this.channel = channel;
		sequenceNumberCounter = new AtomicInteger(0);
	}

	public UtpSocket(DatagramChannel channel, short connectionId) {
		this.channel = channel;
		this.receiveConnectionId = connectionId + 1;
		this.sendConnectionId = connectionId;
		sequenceNumberCounter = new AtomicInteger(0);
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}
}
