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

public class PeerConnectionAccepter extends Thread {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerConnectionAccepter.class);

	private final TorrentClient torrentClient;

	private ServerSocket serverSocket;


	public PeerConnectionAccepter(TorrentClient torrentClient) throws IOException {
		super("Peer connector");
		setDaemon(true);
		this.torrentClient = torrentClient;
		serverSocket = new ServerSocket(torrentClient.getDownloadPort());
	}

	@Override
	public void run() {
		while (true) {
			Socket tcpSocket = null;
			try {
				tcpSocket = serverSocket.accept();

				BitTorrentSocket peerSocket = new BitTorrentSocket(torrentClient.getMessageFactory(), new TcpSocket(tcpSocket));
				BitTorrentHandshake handshake = peerSocket.readHandshake();

				Optional<Torrent> torrent = torrentClient.getTorrentByHash(handshake.getTorrentHash());
				if (!torrent.isPresent()) {
					// We don't know the torrent the peer is downloading
					peerSocket.close();
					continue;
				}

				Peer peer = new Peer(peerSocket, torrent.get(), handshake.getPeerExtensionBytes());
				peerSocket.sendHandshake(torrentClient.getExtensionBytes(), torrentClient.getPeerId(), torrent.get().getHashArray());
				torrent.get().addPeer(peer);
			} catch (IOException e) {
				LOGGER.debug("Failed to create connection with peer.", e);
				closeQuietly(tcpSocket);
			}
		}
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
