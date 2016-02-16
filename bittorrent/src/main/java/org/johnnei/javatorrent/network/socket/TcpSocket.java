package org.johnnei.javatorrent.network.socket;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

public class TcpSocket implements ISocket {

	private Socket socket;

	public TcpSocket() {
		socket = new Socket();
	}

	/**
	 * Creates a TcpSocket on a pre-connected socket
	 *
	 * @param socket The underlying TCP socket
	 */
	public TcpSocket(Socket socket) {
		this.socket = socket;
	}

	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
		socket.connect(endpoint, 10_000);
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
		return socket.isClosed() || !socket.isConnected();
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
	public String toString() {
		return socket.getRemoteSocketAddress().toString().substring(1);
	}

	@Override
	public boolean isConnecting() {
		return false;
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null) {
			return false;
		}
		if (!(o instanceof TcpSocket)) {
			return false;
		}

		TcpSocket other = (TcpSocket) o;
		return Objects.equals(socket, other.socket);
	}

	@Override
	public int hashCode() {
		return Objects.hash(socket);
	}
}
