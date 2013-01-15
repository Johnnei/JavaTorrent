package torrent.download.tracker;

import java.io.IOException;

import org.johnnei.utils.ThreadUtils;

import torrent.download.Torrent;
import torrent.download.peer.Peer;

public class PeerConnectorThread extends Thread {
	
	private final Object LOCK = new Object(); 
	/**
	 * List of peer that are currently being connected
	 */
	private Peer[] peer;
	private int peerCount;
	/**
	 * The torrents to which the connecting peers should be send to
	 */
	private Torrent torrent;
	
	public PeerConnectorThread(Torrent torrent, int maxConnecting) {
		super(torrent + " ConnectThread");
		peer = new Peer[maxConnecting];
		this.torrent = torrent;
	}
	
	/**
	 * Adds a pending connection peer to the connection cycle
	 * @param p The peer to connect
	 */
	public void addPeer(Peer p) {
		for(int i = 0; i < peer.length; i++) {
			if(peer[i] == null) {
				peer[i] = p;
				break;
			}
		}
		addToPeerCount(1);
	}
	
	public void run() {
		while(true) {
			while(peerCount == 0) {
				ThreadUtils.sleep(100);
			}
			for(int i = 0; i < peer.length; i++) {
				if(peer[i] != null) {
					Peer p = peer[i];
					try {
						p.connect();
						if(!p.closed()) {
							p.sendHandshake();
							torrent.addPeer(p);
						}
					} catch (IOException e) {
						p.close();
						p.log(e.getMessage(), true);
					}
					peer[i] = null;
					addToPeerCount(-1);
				}
			}
		}
	}
	
	private synchronized void addToPeerCount(int i) {
		synchronized (LOCK) {
			peerCount += i;
		}
	}
	
	public int getFreeCapacity() {
		return peer.length - peerCount;
	}
	
	public int getConnectingCount() {
		return peerCount;
	}

	public int getMaxCapacity() {
		return peer.length;
	}

}
