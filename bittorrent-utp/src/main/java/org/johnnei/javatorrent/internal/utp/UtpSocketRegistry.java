package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;

import org.johnnei.javatorrent.internal.utils.CheckedSupplier;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocolViolationException;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

public class UtpSocketRegistry {

	private final CheckedSupplier<DatagramChannel, IOException> channelSupplier;

	private final Map<Short, UtpSocket> socketMap;

	public UtpSocketRegistry() {
		channelSupplier = DatagramChannel::open;
		socketMap = new HashMap<>();
	}

	public UtpSocket getSocket(short connectionId) {
		UtpSocket socket = socketMap.get(connectionId);
		if (socket == null) {
			throw new UtpProtocolViolationException(String.format(
				"Packet received for [%s] but no socket has been registered.",
				Short.toUnsignedInt(connectionId)
			));
		}

		return socket;
	}

	public UtpSocket createSocket(SocketAddress socketAddress, UtpPacket synPacket) {
		try {
			DatagramChannel channel = channelSupplier.get();
			channel.connect(socketAddress);
			return UtpSocket.createRemoteConnecting(channel, synPacket);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to bind socket.", e);
		}
	}
}
