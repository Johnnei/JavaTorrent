package torrent.network.utp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;


public class UtpServerSocket extends ServerSocket {

	public UtpServerSocket(int port) throws IOException {
		super(port);
	}
	
	@Override
	/**
	 * Accepts a connection based on a uTP Socket
	 */
	public Socket accept() throws IOException {
		UtpSocket s = new UtpSocket(false);
		implAccept(s);
		s.accepted();
		return s;
	}

}
