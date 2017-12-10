package org.johnnei.javatorrent.internal.network.connector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentProtocolViolationException;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.network.socket.TcpSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class NioConnectionAcceptor {

	private static final Logger LOGGER = LoggerFactory.getLogger(NioConnectionAcceptor.class);

	private final TorrentClient torrentClient;

	private final ServerSocketChannel serverChannel;
	private final ScheduledFuture<?> poller;

	public NioConnectionAcceptor(TorrentClient torrentClient) {
		this.torrentClient = torrentClient;
		try {
			serverChannel = ServerSocketChannel.open();
			serverChannel.bind(new InetSocketAddress(torrentClient.getDownloadPort()));
			serverChannel.configureBlocking(false);
			poller = torrentClient.getExecutorService().scheduleWithFixedDelay(this::pollConnections, 50, 100, TimeUnit.MILLISECONDS);
		} catch (IOException e) {
			throw new IllegalStateException("Failed to set up Nio socket listener.", e);
		}

		LOGGER.info("Listening for incoming peer on TCP port {}", torrentClient.getDownloadPort());
	}

	public void stop() {
		poller.cancel(false);
	}

	private void pollConnections() {
		SocketChannel channel;
		try {
			while ((channel = serverChannel.accept()) != null) {
				onConnectionAccepted(channel);
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to accept connection", e);
		}

	}

	private void onConnectionAccepted(SocketChannel channel) throws IOException {
		try {
			BitTorrentSocket socket = createSocket(new TcpSocket(channel.socket()));
			acceptConnection(socket);
		} catch (BitTorrentProtocolViolationException e) {
			LOGGER.debug("Disconnection client {} due to protocol violation", e);
			channel.close();
		}
	}

	private void acceptConnection(BitTorrentSocket peerSocket) throws IOException {
		BitTorrentHandshake handshake = peerSocket.readHandshake();

		Optional<Torrent> torrent = torrentClient.getTorrentByHash(handshake.getTorrentHash());
		if (!torrent.isPresent()) {
			throw new BitTorrentProtocolViolationException("Peer connection was for unknown torrent.");
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
}
