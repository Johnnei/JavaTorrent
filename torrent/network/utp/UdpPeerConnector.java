package torrent.network.utp;

import java.io.IOException;
import java.util.ArrayList;

import torrent.download.peer.Peer;

public class UdpPeerConnector extends Thread {
	
	private ArrayList<Peer> peers;

	public UdpPeerConnector() throws IOException {
		super("UdpPeerConnector");
		peers = new ArrayList<>();
	}
	
	public void addPeer(Peer p) {
		if(!peers.contains(p)) {
			synchronized (this) {
				peers.add(p);
			}
		}
	}

	public void run() {
		while (true) {
			try {
				for(int i = 0; i < peers.size(); i++) {
					Peer peer = peers.get(0);
					peer.getSocket().checkForPackets();
					peer.getSocket().checkForSendingPackets();
					if (peer.canReadMessage()) {
						peer.processHandshake();
						if (peer.getPassedHandshake()) {
							peer.sendHandshake();
							peer.getTorrent().addPeer(peer);
							synchronized (this) {
								peers.remove(i--);
							}
						} else {
							peer.close();
						}
					} else {
						peer.close();
					}
				}
			} catch (IOException e) {
			}
		}
	}

}