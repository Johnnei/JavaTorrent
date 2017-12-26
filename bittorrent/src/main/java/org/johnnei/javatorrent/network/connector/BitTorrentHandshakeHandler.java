package org.johnnei.javatorrent.network.connector;

import org.johnnei.javatorrent.network.socket.ISocket;

/**
 * Handler which processes freshly connected peers to initiate the BitTorrent protocol.
 */
public interface BitTorrentHandshakeHandler {

	/**
	 * Initiates the handshake by announcing which torrent we expect the remote end to download.
	 * @param socket The channel of the peer.
	 * @param torrentHash The torrent for which we want this peer.
	 */
	void onConnectionEstablished(ISocket socket, byte[] torrentHash);

	/**
	 * Responds to the received connection with the expected torrent if we have it.
	 * @param socket The channel of the peer.
	 */
	void onConnectionReceived(ISocket socket);

}
