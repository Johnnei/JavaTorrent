package torrent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.protocol.TcpSocket;
import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.download.tracker.TrackerManager;
import torrent.network.BitTorrentSocket;
import torrent.protocol.BitTorrentHandshake;
import torrent.protocol.BitTorrentUtil;
import torrent.util.StringUtil;

public class PeerConnectionAccepter extends Thread {

	private final TorrentClient torrentClient;

	private ServerSocket serverSocket;

	private TorrentManager torrentManager;

	private TrackerManager trackerManager;

	public PeerConnectionAccepter(TorrentClient torrentClient, TorrentManager manager, TrackerManager trackerManager) throws IOException {
		super("Peer connector");
		setDaemon(true);
		this.torrentClient = torrentClient;
		this.torrentManager = manager;
		this.trackerManager = trackerManager;
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

				Torrent torrent = torrentManager.getTorrent(StringUtil.byteArrayToString(handshake.getTorrentHash()));

				if (torrent == null) {
					// We don't know the torrent the peer is downloading
					peerSocket.close();
					continue;
				}

				Peer peer = new Peer(peerSocket, torrent);
				peer.getExtensions().register(handshake.getPeerExtensionBytes());
				peer.setTorrent(torrent);
				peerSocket.sendHandshake(trackerManager.getPeerId(), torrent.getHashArray());
				BitTorrentUtil.onPostHandshake(peer);
			} catch (IOException e) {
			}
		}
	}

}
