package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.net.ServerSocket;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.network.socket.ISocket;
import org.johnnei.javatorrent.internal.network.socket.TcpSocket;

public class TcpPeerConnectionAcceptor extends AbstractPeerConnectionAcceptor {

	private ServerSocket serverSocket;

	public TcpPeerConnectionAcceptor(TorrentClient torrentClient) throws IOException {
		super(torrentClient);
		serverSocket = createServerSocket();
	}

	@Override
	protected ISocket acceptSocket() throws IOException {
		return new TcpSocket( serverSocket.accept());
	}

	ServerSocket createServerSocket() throws IOException {
		return new ServerSocket(torrentClient.getDownloadPort());
	}

}
