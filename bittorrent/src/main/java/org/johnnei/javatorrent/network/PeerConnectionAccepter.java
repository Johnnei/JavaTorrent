package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentUtil;
import org.johnnei.javatorrent.network.socket.TcpSocket;
import org.johnnei.javatorrent.torrent.TorrentManager;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.tracker.TrackerManager;
import org.johnnei.javatorrent.utils.ThreadUtils;

public class PeerConnectionAccepter extends Thread {

	private final TorrentClient torrentClient;

	private final TorrentManager torrentManager;

	private final TrackerManager trackerManager;

	private ServerSocket serverSocket;


	public PeerConnectionAccepter(TorrentClient torrentClient) throws IOException {
		super("Peer connector");
		setDaemon(true);
		this.torrentClient = torrentClient;
		this.torrentManager = torrentClient.getTorrentManager();
		this.trackerManager = torrentClient.getTrackerManager();
		serverSocket = new ServerSocket(torrentClient.getDownloadPort());
	}

	@Override
	public void run() {
		while (true) {
			try {
				Socket tcpSocket = serverSocket.accept();

				BitTorrentSocket peerSocket = new BitTorrentSocket(torrentClient.getMessageFactory(), new TcpSocket(tcpSocket));

				long handshakeStart = System.currentTimeMillis();
				while (!peerSocket.canReadMessage() && (System.currentTimeMillis() - handshakeStart) < 5000) {
					ThreadUtils.sleep(10);
				}
				if (!peerSocket.canReadMessage()) {
					peerSocket.close();
					continue;
				}

				BitTorrentHandshake handshake = peerSocket.readHandshake();

				Optional<Torrent> torrent = torrentManager.getTorrent(handshake.getTorrentHash());
				if (!torrent.isPresent()) {
					// We don't know the torrent the peer is downloading
					peerSocket.close();
					continue;
				}

				Peer peer = new Peer(peerSocket, torrent.get(), handshake.getPeerExtensionBytes());
				peerSocket.sendHandshake(torrentClient.getExtensionBytes(), trackerManager.getPeerId(), torrent.get().getHashArray());
				BitTorrentUtil.onPostHandshake(peer);
			} catch (IOException e) {
			}
		}
	}

}
