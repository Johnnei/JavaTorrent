package org.johnnei.javatorrent.network.socket;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;
import org.johnnei.javatorrent.utp.UtpModule;

public class UtpSocket implements ISocket {

	private UtpSocketImpl.Builder socketFactory;

	private UtpSocketImpl socket;

	private UtpMultiplexer multiplexer;

	/**
	 * Creates a new socket.
	 *
	 * Instances of UtpSocket must be created using {@link UtpModule#createSocketFactory()}
	 *
	 * @param utpMultiplexer
	 *
	 * @see UtpModule#createSocketFactory()
	 */
	public UtpSocket(UtpMultiplexer utpMultiplexer) {
		multiplexer = utpMultiplexer;
		socketFactory = new UtpSocketImpl.Builder()
				.setUtpMultiplexer(multiplexer);
	}

	public UtpSocket(UtpMultiplexer utpMultiplexer, UtpSocketImpl socket) {
		this.multiplexer = utpMultiplexer;
		this.socket = socket;
	}

	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
		socketFactory.setSocketAddress(endpoint);
		do {
			socket = socketFactory.build();
		} while (!multiplexer.registerSocket(socket));

		socket.connect(endpoint);
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return socket.getInputStream();
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return socket.getOutputStream();
	}

	@Override
	public void close() throws IOException {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isClosed() {
		return socket == null || socket.getConnectionState() == ConnectionState.CLOSED;
	}

	@Override
	public boolean isInputShutdown() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isOutputShutdown() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void flush() throws IOException {
		throw new UnsupportedOperationException();
	}
}
