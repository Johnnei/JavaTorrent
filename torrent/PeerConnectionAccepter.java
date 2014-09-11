package torrent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.download.Torrent;
import torrent.download.peer.Peer;
import torrent.network.protocol.TcpSocket;
import torrent.protocol.BitTorrentHandshake;
import torrent.protocol.BitTorrentUtil;
import torrent.util.StringUtil;

public class PeerConnectionAccepter extends Thread {

	private ServerSocket serverSocket;
	
	private TorrentManager manager;

	public PeerConnectionAccepter(TorrentManager manager) throws IOException {
		super("Peer connector");
		this.manager = manager;
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
				
				Torrent torrent = manager.getTorrent(StringUtil.byteArrayToString(handshake.getTorrentHash()));
				
				if (torrent == null) {
					// We don't know the torrent the peer is downloading
					peer.close();
					continue;
				}
				
				peer.getClient().setReservedBytes(handshake.getPeerExtensionBytes());
				peer.setTorrent(torrent);
				peer.sendHandshake(manager.getTrackerManager().getPeerId());
				BitTorrentUtil.onPostHandshake(peer);
				peer.getTorrent().addPeer(peer);
			} catch (IOException e) {
			}
		}
	}

}
