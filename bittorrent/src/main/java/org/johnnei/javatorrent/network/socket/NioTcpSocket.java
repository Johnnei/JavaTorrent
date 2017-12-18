package org.johnnei.javatorrent.network.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;

public class NioTcpSocket implements ISocket {

	private SocketChannel channel;

	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
		channel = SocketChannel.open(endpoint);
		channel.configureBlocking(false);
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
