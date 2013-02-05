package torrent.network.utp;

import java.io.IOException;
import java.util.ArrayList;

import org.johnnei.utils.ThreadUtils;

import torrent.download.peer.Peer;

public class UdpPeerConnector extends Thread {

	private ArrayList<Peer> peers;

	public UdpPeerConnector() throws IOException {
		super("UdpPeerConnector");
		peers = new ArrayList<>();
	}

	public boolean addPeer(Peer p) {
		if (!peers.contains(p)) {
			synchronized (this) {
				peers.add(p);
				System.out.println("[UdpConnector] Added Peer, new Peer Count: " + peers.size() + ", Peer: " + p);
			}
			return true;
		}
		return false;
	}

	public void run() {
		while (true) {
			for (int i = 0; i < peers.size(); i++) {
				try {
					Peer peer = peers.get(i);
					peer.getSocket().checkForPackets();
					peer.getSocket().checkForSendingPackets();
					if (peer.canReadMessage()) {
						peer.processHandshake();
						if (peer.getPassedHandshake()) {
							peer.sendHandshake();
							peer.getTorrent().addPeer(peer);
							synchronized (this) {
								peers.remove(i--);
								System.out.println("[UdpConnector] Peer is connected, new Peer Count: " + peers.size() + ", Peer: " + peer);
							}
						} else {
							peer.close();
						}
					} else {
						peer.close();
					}
					ThreadUtils.sleep(1);
				} catch (IOException e) {
				}
			}
			ThreadUtils.sleep(1);
		}
	}

}