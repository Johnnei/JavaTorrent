package torrent.network.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TcpSocket implements ISocket {

	private Socket socket;
	
	public TcpSocket() {
		socket = new Socket();
	}
	
	@Override
	public void connect(InetSocketAddress endpoint)
			throws IOException {
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
	public ISocket getFallbackSocket() {
		return null;
	}

	@Override
	public boolean canFallback() {
		return false;
	}

	@Override
	public boolean isClosed() {
		return socket.isClosed();
	}

	@Override
	public boolean isInputShutdown() {
		return socket.isInputShutdown();
	}

	@Override
	public boolean isOutputShutdown() {
		return socket.isOutputShutdown();
	}

}
