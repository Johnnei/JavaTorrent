package torrent.network.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import torrent.network.protocol.utp.UtpClient;

public class UtpSocket implements ISocket, Comparable<UtpSocket> {

	private UtpClient myClient;
	private UtpClient peerClient;
	
	public UtpSocket() {
		myClient = new UtpClient();
		peerClient = new UtpClient();
	}
	
	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public InputStream getInputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void close() throws IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ISocket getFallbackSocket() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canFallback() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isClosed() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isInputShutdown() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isOutputShutdown() {
		// TODO Auto-generated method stub
		return false;
	}
	
	public UtpClient getMyClient() {
		return myClient;
	}
	
	public UtpClient getPeerClient() {
		return peerClient;
	}

	@Override
	public int compareTo(UtpSocket otherSocket) {
		return peerClient.getConnectionId() - otherSocket.getPeerClient().getConnectionId();
	}

}
