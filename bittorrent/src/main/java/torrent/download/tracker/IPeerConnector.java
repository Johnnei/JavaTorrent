package torrent.download.tracker;

import torrent.download.Torrent;
import torrent.download.peer.PeerConnectInfo;

public interface IPeerConnector {

	/**
	 * Queues a peer to be connected
	 * @param p the peer to be connected
	 */
	void connectPeer(PeerConnectInfo peer);

	int getConnectingCountFor(Torrent torrent);

}
