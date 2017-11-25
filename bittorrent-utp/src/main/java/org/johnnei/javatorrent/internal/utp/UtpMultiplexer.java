package org.johnnei.javatorrent.internal.utp;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocolViolationException;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;
import org.johnnei.javatorrent.internal.utp.stream.PacketReader;

public class UtpMultiplexer implements Closeable, Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpMultiplexer.class);

	private static final int BUFFER_SIZE = 32_768;

	private final PacketReader packetReader;

	private final UtpSocketRegistry socketRegistry;

	private final UtpPeerConnectionAcceptor connectionAcceptor;

	private final LoopingRunnable connectionAcceptorRunnable;

	private final Thread connectionAcceptorThread;

	private DatagramChannel channel;

	public UtpMultiplexer(UtpPeerConnectionAcceptor connectionAcceptor, PacketReader packetReader, int port) throws IOException {
		this.connectionAcceptor = connectionAcceptor;
		this.packetReader = packetReader;
		connectionAcceptorRunnable = new LoopingRunnable(connectionAcceptor);
		connectionAcceptorThread = new Thread(connectionAcceptorRunnable, "uTP Connection Acceptor");
		channel = DatagramChannel.open();
		channel.bind(new InetSocketAddress(port));
		channel.configureBlocking(true);
		socketRegistry = new UtpSocketRegistry(channel);
		connectionAcceptorThread.start();
		LOGGER.trace("Configured to listen on {}", channel.getLocalAddress());
	}

	@Override
	public void run() {
		// Receive message
		ByteBuffer buffer;
		SocketAddress socketAddress;
		try {
			buffer = ByteBuffer.allocate(BUFFER_SIZE);
			socketAddress = channel.receive(buffer);
			buffer.flip();
		} catch (IOException e) {
			LOGGER.error("Failed to read message.", e);
			return;
		}

		try {
			// Transform message
			UtpPacket packet = packetReader.read(buffer);

			// Retrieve socket
			UtpSocket socket;
			if (packet.getHeader().getType() == PacketType.SYN.getTypeField()) {
				LOGGER.debug("Received connection with id [{}]", Short.toUnsignedInt(packet.getHeader().getConnectionId()));
				socket = socketRegistry.createSocket(socketAddress, packet);
				connectionAcceptor.onReceivedConnection(socket);
			} else {
				socket = socketRegistry.getSocket(packet.getHeader().getConnectionId());
			}

			socket.onReceivedPacket(packet);
		} catch (UtpProtocolViolationException e) {
			LOGGER.trace("uTP protocol was violated.", e);
		}
	}

	public ISocket createUnconnectedSocket() {
		return socketRegistry.allocateSocket(connectionId -> UtpSocket.createInitiatingSocket(channel, connectionId));
	}

	public void updateSockets() {
		try {
			for (UtpSocket socket : socketRegistry.getAllSockets()) {
				socket.processSendQueue();
				socket.processTimeout();
			}
			socketRegistry.removeShutdownSockets();
		} catch (Exception e) {
			LOGGER.warn("uTP socket caused exception", e);
		}
	}

	@Override
	public void close() throws IOException {
		channel.close();
		connectionAcceptorRunnable.stop();
		try {
			connectionAcceptorThread.interrupt();
			connectionAcceptorThread.join();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.warn("Interrupted while waiting for connection acceptor thread to shutdown.", e);
		}
	}
}
