package torrent.download;

import java.io.IOException;
import java.util.ArrayList;

import org.johnnei.utils.ThreadUtils;

import torrent.download.peer.Peer;

public class PeersWriteThread extends Thread {

	private Torrent torrent;

	public PeersWriteThread(Torrent torrent) {
		super(torrent + " WriteThread");
		this.torrent = torrent;
	}

	@Override
	public void run() {
		while (true) {
			ArrayList<Peer> peers = torrent.getPeers();
			for (int i = 0; i < peers.size(); i++) {
				Peer p = peers.get(i);
				if (p == null)
					continue;
				if (!p.closed()) {
					try {
						p.sendMessage();
					} catch (IOException e) {
						p.log(e.getMessage(), true);
						p.close();
					}
				}
			}
			ThreadUtils.sleep(1);
		}
	}
}
