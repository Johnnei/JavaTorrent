package torrent;

import java.io.IOException;
import java.net.ServerSocket;

import org.johnnei.utils.ThreadUtils;

import torrent.download.peer.Peer;
import torrent.network.utp.UtpSocket;

public class PeerConnector extends Thread {

	private ServerSocket serverSocket;

	public PeerConnector() throws IOException {
		super("PeerConnector");
		serverSocket = new UtpServerSocket(27960);
	}

	public void run() {
		while (true) {
			try {
				Peer peer = new Peer();
				UtpSocket peerSocket = (UtpSocket) serverSocket.accept();
				peer.setSocket(peerSocket);
				long handshakeStart = System.currentTimeMillis();
				while (!peer.canReadMessage() && (System.currentTimeMillis() - handshakeStart) < 5000) {
					ThreadUtils.sleep(10);
				}
				if (peer.canReadMessage()) {
					peer.processHandshake();
					if (peer.getPassedHandshake()) {
						peer.sendHandshake();
						peer.getTorrent().addPeer(peer);
					} else {
						peer.close();
					}
				} else {
					peer.close();
				}
			} catch (IOException e) {
			}
		}
	}

}
