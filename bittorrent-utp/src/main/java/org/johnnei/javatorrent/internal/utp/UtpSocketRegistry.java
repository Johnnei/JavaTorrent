package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;

import org.johnnei.javatorrent.internal.utils.CheckedSupplier;

public class UtpSocketRegistry {

	private final CheckedSupplier<DatagramChannel, IOException> channelSupplier;

	private final Map<Short, UtpSocket> socketMap;

	public UtpSocketRegistry() {
		channelSupplier = DatagramChannel::open;
		socketMap = new HashMap<>();
	}

	public UtpSocket getSocket(SocketAddress socketAddress, short connectionId) {
		return socketMap.computeIfAbsent(connectionId, id -> createSocket(socketAddress, id));
	}

	private UtpSocket createSocket(SocketAddress socketAddress, short connectionId) {
		try {
			DatagramChannel channel = channelSupplier.get();
			channel.connect(socketAddress);
			return new UtpSocket(channel, connectionId);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to bind socket.", e);
		}
	}
}
