package torrent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.johnnei.utils.ThreadUtils;
import org.johnnei.utils.config.Config;

import torrent.download.peer.Peer;
import torrent.network.protocol.TcpSocket;

public class PeerConnectionAccepter extends Thread {

	private ServerSocket serverSocket;
	
	private Manager manager;

	public PeerConnectionAccepter(Manager manager) throws IOException {
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
				if (peer.canReadMessage()) {
					// TODO Update to new API
					peer.processHandshake();
					if (peer.getPassedHandshake()) {
						peer.sendHandshake(manager.getTrackerManager().getPeerId());
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
