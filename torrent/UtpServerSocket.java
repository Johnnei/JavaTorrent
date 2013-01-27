package torrent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import torrent.network.UtpSocket;

public class UtpServerSocket extends ServerSocket {

	public UtpServerSocket(int port) throws IOException {
		super(port);
	}
	
	@Override
	/**
	 * Accepts a connection based on a uTP Socket
	 */
	public Socket accept() throws IOException {
		Socket s = new UtpSocket();
		implAccept(s);
		return s;
	}

}
