package org.johnnei.javatorrent.internal.utp;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.TorrentClientSettings;
import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.internal.utils.CheckedSupplier;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocolViolationException;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;
import org.johnnei.javatorrent.internal.utp.stream.PacketReader;
import org.johnnei.javatorrent.network.socket.ISocket;

public class UtpMultiplexer implements Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpMultiplexer.class);

	private static final int BUFFER_SIZE = 32_768;

	private final PacketReader packetReader;

	private final UtpSocketRegistry socketRegistry;

	private final UtpPeerConnectionAcceptor connectionAcceptor;

	private final LoopingRunnable connectionAcceptorRunnable;

	private final Thread connectionAcceptorThread;

	private final DatagramChannel channel;

	private final Future<?> poller;

	private final TorrentClientSettings clientSettings;

	public UtpMultiplexer(Builder builder) throws IOException {
		this.connectionAcceptor = builder.connectionAcceptor;
		this.packetReader = builder.packetReader;
		this.clientSettings = builder.client.getSettings();

		channel = builder.channelFactory.get();
		channel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
		channel.bind(new InetSocketAddress(clientSettings.getAcceptingPort()));
		channel.configureBlocking(false);
		LOGGER.trace("Configured to listen on {}", channel.getLocalAddress());
		socketRegistry = new UtpSocketRegistry(channel);

		if (clientSettings.isAcceptingConnections()) {
			connectionAcceptorRunnable = new LoopingRunnable(connectionAcceptor);
			connectionAcceptorThread = new Thread(connectionAcceptorRunnable, "uTP Connection Acceptor");
			connectionAcceptorThread.start();
		} else {
			connectionAcceptorRunnable = null;
			connectionAcceptorThread = null;
		}


		poller = builder.client.getExecutorService().scheduleWithFixedDelay(this::pollPackets, 50, 10, TimeUnit.MILLISECONDS);
	}

	void pollPackets() {
		try {
			ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
			SocketAddress socketAddress = channel.receive(buffer);

			if (socketAddress == null) {
				return;
			}

			buffer.flip();
			onPacketReceived(socketAddress, buffer);
		} catch (IOException e) {
			LOGGER.warn("Failed to process uTP packets.", e);
		}
	}

	private void onPacketReceived(SocketAddress socketAddress, ByteBuffer buffer) {
		try {
			UtpPacket packet = packetReader.read(buffer);
			findSocketForPacket(socketAddress, packet)
				.ifPresent(socket -> socket.onReceivedPacket(packet));
		} catch (UtpProtocolViolationException e) {
			LOGGER.trace("uTP protocol was violated.", e);
		}
	}

	private Optional<UtpSocket> findSocketForPacket(SocketAddress socketAddress, UtpPacket packet) {
		if (packet.getHeader().getType() == PacketType.SYN.getTypeField()) {
			if (clientSettings.isAcceptingConnections()) {
				LOGGER.debug("Received connection with id [{}]", Short.toUnsignedInt(packet.getHeader().getConnectionId()));
				UtpSocket socket = socketRegistry.createSocket(socketAddress, packet);
				connectionAcceptor.onReceivedConnection(socket);
				return Optional.of(socket);
			} else {
				return Optional.empty();
			}
		} else {
			return Optional.of(socketRegistry.getSocket(packet.getHeader().getConnectionId()));
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

	public void close() throws IOException {
		poller.cancel(false);
		channel.close();
		if (connectionAcceptorRunnable != null) {
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

	public static final class Builder {

		private final TorrentClient client;

		private final PacketReader packetReader;

		private UtpPeerConnectionAcceptor connectionAcceptor;

		private CheckedSupplier<DatagramChannel, IOException> channelFactory = DatagramChannel::open;

		public Builder(TorrentClient client, PacketReader packetReader) {
			this.client = client;
			this.packetReader = packetReader;
		}

		public Builder withConnectionAcceptor(UtpPeerConnectionAcceptor connectionAcceptor) {
			this.connectionAcceptor = connectionAcceptor;
			return this;
		}

		public Builder withChannelFactory(CheckedSupplier<DatagramChannel, IOException> channelFactory) {
			this.channelFactory = channelFactory;
			return this;
		}

		public UtpMultiplexer build() throws IOException {
			if (client.getSettings().isAcceptingConnections()) {
				Objects.requireNonNull(connectionAcceptor, "Connection Acceptor is required when accepting connections");
			}
			return new UtpMultiplexer(this);
		}
	}
}
