package org.johnnei.javatorrent.internal.network.socket;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;
import org.johnnei.javatorrent.utp.UtpModule;

/**
 * Socket which implements the {@link ISocket} facade for uTP connections.
 */
public class UtpSocket implements ISocket {

	private UtpSocketImpl.Builder socketFactory;

	private UtpSocketImpl socket;

	private UtpMultiplexer multiplexer;

	/**
	 * Creates a new socket.
	 *
	 * Instances of UtpSocket must be created using {@link UtpModule#createSocketFactory()}
	 *
	 * @param utpMultiplexer The multiplexer on which this socket will register.
	 *
	 * @see UtpModule#createSocketFactory()
	 */
	public UtpSocket(UtpMultiplexer utpMultiplexer) {
		multiplexer = utpMultiplexer;
		socketFactory = new UtpSocketImpl.Builder()
				.setUtpMultiplexer(multiplexer);
	}

	/**
	 * Creates a new socket based on a received connection.
	 * @param utpMultiplexer The multiplexer on which this socket is registered.
	 * @param socket The socket.
	 */
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
		socket.close();
	}

	@Override
	public boolean isClosed() {
		return socket == null || socket.getConnectionState() == ConnectionState.CLOSED;
	}

	@Override
	public boolean isInputShutdown() {
		return socket.isInputShutdown();
	}

	@Override
	public boolean isOutputShutdown() {
		return socket.isOutputShutdown();
	}

	@Override
	public void flush() throws IOException {
		getOutputStream().flush();
	}
}
