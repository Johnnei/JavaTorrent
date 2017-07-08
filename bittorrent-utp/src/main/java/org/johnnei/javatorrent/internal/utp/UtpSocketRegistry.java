package org.johnnei.javatorrent.internal.utp;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.johnnei.javatorrent.internal.utils.CheckedSupplier;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocolViolationException;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

public class UtpSocketRegistry {

	private final Object createLock = new Object();

	private final CheckedSupplier<DatagramChannel, IOException> channelSupplier;

	private final Map<Short, UtpSocket> socketMap;

	private final Random random;

	public UtpSocketRegistry() {
		channelSupplier = DatagramChannel::open;
		socketMap = new HashMap<>();
		random = new Random();
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

	/**
	 * Takes a function which creates, and stores, a socket based on the allocated connection id.
	 * @param socketSupplier The function which is capable of creating a socket.
	 * @return The allocated socket.
	 */
	public UtpSocket allocateSocket(Function<Short, UtpSocket> socketSupplier) {
		synchronized (createLock) {
			// FIXME Infinite loop when all IDs are allocated.
			UtpSocket socket = null;
			do {
				short receiveId = (short) random.nextInt();
				if (!socketMap.containsKey(receiveId)) {
					socket = socketSupplier.apply(receiveId);
					socketMap.put(receiveId, socket);
				}
			} while (socket == null);
			return socket;
		}
	}

	public UtpSocket createSocket(SocketAddress socketAddress, UtpPacket synPacket) {
		synchronized (createLock) {
			if (socketMap.containsKey(synPacket.getHeader().getConnectionId())) {
				throw new UtpProtocolViolationException(String.format("Connection [%s] already registered before.", synPacket.getHeader().getConnectionId()));
			}

			try {
				DatagramChannel channel = channelSupplier.get();
				channel.connect(socketAddress);
				UtpSocket socket = UtpSocket.createRemoteConnecting(channel, synPacket);
				socketMap.put(synPacket.getHeader().getConnectionId(), socket);
				return socket;
			} catch (IOException e) {
				throw new IllegalStateException("Failed to bind socket.", e);
			}
		}
	}

	public Collection<UtpSocket> getAllSockets() {
		synchronized (createLock) {
			return new ArrayList<>(socketMap.values());
		}
	}
}
