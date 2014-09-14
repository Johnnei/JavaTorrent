package torrent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.download.tracker.TrackerManager;
import torrent.network.protocol.TcpSocket;
import torrent.protocol.BitTorrentHandshake;
import torrent.protocol.BitTorrentUtil;
import torrent.util.StringUtil;

public class PeerConnectionAccepter extends Thread {

	private ServerSocket serverSocket;
	
	private TorrentManager torrentManager;
	
	private TrackerManager trackerManager;

	public PeerConnectionAccepter(TorrentManager manager, TrackerManager trackerManager) throws IOException {
		super("Peer connector");
		setDaemon(true);
		this.torrentManager = manager;
		this.trackerManager = trackerManager;
		serverSocket = new ServerSocket(Config.getConfig().getInt("download-port"));
	}

	public void run() {
		while (true) {
			try {
				Peer peer = new Peer();
				Socket peerSocket = (Socket) serverSocket.accept();
				peer.setSocket(new TcpSocket(peerSocket));
				long handshakeStart = System.currentTimeMillis();
				while (!peer.canReadMessage() && (System.currentTimeMillis() - handshakeStart) < 5000) {
					ThreadUtils.sleep(10);
				}
				if (!peer.canReadMessage()) {
					peer.close();
					continue;
				}
					
				BitTorrentHandshake handshake = peer.readHandshake();
				
				Torrent torrent = torrentManager.getTorrent(StringUtil.byteArrayToString(handshake.getTorrentHash()));
				
				if (torrent == null) {
					// We don't know the torrent the peer is downloading
					peer.close();
					continue;
				}
				
				peer.getExtensions().register(handshake.getPeerExtensionBytes());
				peer.setTorrent(torrent);
				peer.sendHandshake(trackerManager.getPeerId());
				BitTorrentUtil.onPostHandshake(peer);
				peer.getTorrent().addPeer(peer);
			} catch (IOException e) {
			}
		}
	}

}
