package org.johnnei.javatorrent.network.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class NioTcpSocket implements ISocket {

	private final SocketChannel channel;

	public NioTcpSocket(SocketChannel channel) {
		if (channel.isBlocking()) {
			throw new IllegalArgumentException("Only non-blocking Channels are supported.");
		}
		this.channel = channel;
	}

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
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}

	@Override
	public SocketChannel getChannel() {
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
		return false;
	}

	@Override
	public boolean isOutputShutdown() {
		return false;
	}

	@Override
	public void flush() throws IOException {

	}
}
