package torrent.download;

import java.io.IOException;
import java.util.ArrayList;

import org.johnnei.utils.ThreadUtils;

import torrent.download.peer.Peer;

public class PeersReadThread extends Thread {

	private Torrent torrent;

	public PeersReadThread(Torrent torrent) {
		super(torrent + " ReadThread");
		this.torrent = torrent;
	}

	@Override
	public void run() {
		while (true) {
			ArrayList<Peer> peers = torrent.getPeers();
			for (int i = 0; i < peers.size(); i++) {
				Peer p = peers.get(i);
				if (!p.closed()) {
					try {
						if (p.canReadMessage()) {
							p.readMessage();
						}
					} catch (IOException e) {
						p.close();
					}
				}
			}
			ThreadUtils.sleep(1);
		}
	}

}
