package org.johnnei.javatorrent.network.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default TCP Socket implementation which uses NIO as the underlying platform.
 */
public class NioTcpSocket implements ISocket {

	private static final Logger LOGGER = LoggerFactory.getLogger(NioTcpSocket.class);

	private final SocketChannel channel;

	/**
	 * Creates a new Socket with an pre-exisiting channel. The channel must be in non-blocking mode.
	 * @param channel The pre-exisiting channel.
	 * @throws IllegalArgumentException When the channel is in blocking mode.
	 */
	public NioTcpSocket(SocketChannel channel) {
		if (channel.isBlocking()) {
			throw new IllegalArgumentException("Only non-blocking Channels are supported.");
		}
		this.channel = channel;
	}

	/**
	 * Creates a new non-blocking TCP Socket.
	 */
	public NioTcpSocket() {
		try {
			channel = SocketChannel.open();
			channel.configureBlocking(false);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to create socket channel", e);
		}
	}

	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
		channel.connect(endpoint);
	}

	@Override
	public boolean isConnected() {
		if (channel.isConnectionPending()) {
			try {
				channel.finishConnect();
			} catch (IOException e) {
				LOGGER.debug("Failed to connect to remote.", e);
			}
		}

		return channel.isConnected();
	}

	@Override
	public SocketChannel getReadableChannel() {
		return channel;
	}

	@Override
	public SocketChannel getWritableChannel() {
		return channel;
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	@Override
	public boolean isClosed() {
		return !channel.isOpen();
	}

	@Override
	public boolean isInputShutdown() {
		return channel.isOpen();
	}

	@Override
	public boolean isOutputShutdown() {
		return channel.isOpen();
	}

}
