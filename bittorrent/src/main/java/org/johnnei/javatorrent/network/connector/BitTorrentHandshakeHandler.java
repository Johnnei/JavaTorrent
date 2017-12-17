package org.johnnei.javatorrent.network.connector;

import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;

/**
 * Handler which processes freshly connected peers to initiate the BitTorrent protocol.
 */
public interface BitTorrentHandshakeHandler {

	/**
	 * Initiates the handshake by announcing which torrent we expect the remote end to download.
	 * @param channel The channel of the peer.
	 * @param torrentHash The torrent for which we want this peer.
	 * @param <T> A Selectable and Read/Writable socket.
	 */
	<T extends SelectableChannel & ByteChannel> void onConnectionEstablished(T channel, byte[] torrentHash);

	/**
	 * Responds to the received connection with the expected torrent if we have it.
	 * @param channel The channel of the peer.
	 * @param <T> A Selectable and Read/Writable socket.
	 */
	<T extends SelectableChannel & ByteChannel> void onConnectionReceived(T channel);

}
