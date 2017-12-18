package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * Created by johnn on 14/05/2016.
 */
@Deprecated
public abstract class AbstractPeerConnectionAcceptor implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPeerConnectionAcceptor.class);

	protected final TorrentClient torrentClient;

	public AbstractPeerConnectionAcceptor(TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
	}

	@Override
	public void run() {
		ISocket socket = null;
		try {
			socket = acceptSocket();
			acceptConnection(createSocket(socket));
		} catch (IOException e) {
			LOGGER.debug("Failed to create connection with peer.", e);
			closeQuietly(socket);
		}
	}

	protected abstract ISocket acceptSocket() throws IOException;

	private void acceptConnection(BitTorrentSocket peerSocket) throws IOException {
		BitTorrentHandshake handshake = peerSocket.readHandshake();

		Optional<Torrent> torrent = torrentClient.getTorrentByHash(handshake.getTorrentHash());
		if (!torrent.isPresent()) {
			// We don't know the torrent the peer is downloading
			peerSocket.close();
			return;
		}

		Peer peer = createPeer(peerSocket, torrent.get(), handshake.getPeerExtensionBytes(), handshake.getPeerId());
		peerSocket.sendHandshake(torrentClient.getExtensionBytes(), torrentClient.getPeerId(), torrent.get().getMetadata().getHash());
		LOGGER.debug("Accepted connection from {}", peerSocket);
		torrent.get().addPeer(peer);
	}

	BitTorrentSocket createSocket(ISocket socket) throws IOException {
		return new BitTorrentSocket(torrentClient.getMessageFactory(), socket);
	}

	Peer createPeer(BitTorrentSocket socket, Torrent torrent, byte[] extensionBytes, byte[] peerId) {
		return new Peer.Builder()
				.setSocket(socket)
				.setTorrent(torrent)
				.setExtensionBytes(extensionBytes)
				.setId(peerId)
				.build();
	}

	private void closeQuietly(ISocket socket) {
		if (socket == null) {
			return;
		}
		try {
			socket.close();
		} catch (IOException e) {
			LOGGER.debug("Failed to close socket", e);
		}
	}
}
