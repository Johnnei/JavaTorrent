package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.protocol.TcpSocket;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.tracker.TrackerManager;
import org.johnnei.javatorrent.torrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.torrent.protocol.BitTorrentUtil;
import org.johnnei.javatorrent.torrent.util.StringUtil;
import org.johnnei.javatorrent.utils.ThreadUtils;
import org.johnnei.javatorrent.utils.config.Config;

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
		serverSocket = new ServerSocket(Config.getConfig().getInt("download-port"));
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

				Optional<Torrent> torrent = torrentManager.getTorrent(StringUtil.byteArrayToString(handshake.getTorrentHash()));
				if (!torrent.isPresent()) {
					// We don't know the torrent the peer is downloading
					peerSocket.close();
					continue;
				}

				Peer peer = new Peer(peerSocket, torrent.get());
				peer.getExtensions().register(handshake.getPeerExtensionBytes());
				peer.setTorrent(torrent.get());
				peerSocket.sendHandshake(trackerManager.getPeerId(), torrent.get().getHashArray());
				BitTorrentUtil.onPostHandshake(peer);
			} catch (IOException e) {
			}
		}
	}

}
