package org.johnnei.javatorrent.internal.utp;

import java.net.SocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocolViolationException;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

public class UtpSocketRegistry {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpSocketRegistry.class);

	private final Object createLock = new Object();

	private final DatagramChannel channel;

	private final Map<Short, UtpSocket> socketMap;

	private final Random random;

	public UtpSocketRegistry(DatagramChannel channel) {
		this.channel = channel;
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
					LOGGER.trace("Registered initiated socket to receive on id [{}]", Short.toUnsignedInt(receiveId));
					socketMap.put(receiveId, socket);
				}
			} while (socket == null);
			return socket;
		}
	}

	public UtpSocket createSocket(SocketAddress socketAddress, UtpPacket synPacket) {
		synchronized (createLock) {
			short receiveId = (short) (synPacket.getHeader().getConnectionId() + 1);
			if (socketMap.containsKey(receiveId)) {
				throw new UtpProtocolViolationException(String.format("Connection [%s] already registered before.", synPacket.getHeader().getConnectionId()));
			}

			UtpSocket socket = UtpSocket.createRemoteConnecting(channel, synPacket);
			socket.bind(socketAddress);
			LOGGER.trace("Registered received socket on to receive on id [{}] and send to [{}]", Short.toUnsignedInt(receiveId), socketAddress);
			socketMap.put(receiveId, socket);
			return socket;
		}
	}

	public Collection<UtpSocket> getAllSockets() {
		synchronized (createLock) {
			return new ArrayList<>(socketMap.values());
		}
	}

	/**
	 * Removes all shutdown sockets freeing up their allocated IDs.
	 */
	public void removeShutdownSockets() {
		synchronized (createLock) {
			Iterator<Map.Entry<Short, UtpSocket>> iterator = socketMap.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry<Short, UtpSocket> socketEntry = iterator.next();
				UtpSocket socket = socketEntry.getValue();
				if (socket.isShutdown()) {
					LOGGER.trace("Removed registration of socket with receive id [{}]", Short.toUnsignedInt(socketEntry.getKey()));
					iterator.remove();
				}
			}
		}
	}
}
