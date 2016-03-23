package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.network.socket.TcpSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeerConnectionAcceptor implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnectionAcceptor.class);

	private final TorrentClient torrentClient;

	private ServerSocket serverSocket;

	public PeerConnectionAcceptor(TorrentClient torrentClient) throws IOException {
		this.torrentClient = torrentClient;
		serverSocket = createServerSocket();
	}
	@Override
	public void run() {
		Socket tcpSocket = null;
		try {
			tcpSocket = serverSocket.accept();

			BitTorrentSocket peerSocket = createSocket(new TcpSocket(tcpSocket));
			BitTorrentHandshake handshake = peerSocket.readHandshake();

			Optional<Torrent> torrent = torrentClient.getTorrentByHash(handshake.getTorrentHash());
			if (!torrent.isPresent()) {
				// We don't know the torrent the peer is downloading
				peerSocket.close();
				return;
			}

			Peer peer = createPeer(peerSocket, torrent.get(), handshake.getPeerExtensionBytes(), handshake.getPeerId());
			peerSocket.sendHandshake(torrentClient.getExtensionBytes(), torrentClient.getPeerId(), torrent.get().getHashArray());
			torrent.get().addPeer(peer);
		} catch (IOException e) {
			LOGGER.debug("Failed to create connection with peer.", e);
			closeQuietly(tcpSocket);
		}
	}

	ServerSocket createServerSocket() throws IOException {
		return new ServerSocket(torrentClient.getDownloadPort());
	}

	BitTorrentSocket createSocket(TcpSocket socket) throws IOException {
		return new BitTorrentSocket(torrentClient.getMessageFactory(), socket);
	}

	Peer createPeer(BitTorrentSocket socket, Torrent torrent, byte[] extensionBytes, byte[] peerId) {
		return new Peer.Builder()
				.setSocket(socket)
				.setTorrent(torrent)
				.setExtensionBytes(extensionBytes)
				.setId(peerId)
				.build();
	}

	private void closeQuietly(Socket socket) {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (IOException e) {
			LOGGER.debug("Failed to close socket", e);
		}
	}

}
