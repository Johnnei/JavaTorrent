package org.johnnei.javatorrent.internal.network.connector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.Clock;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentProtocolViolationException;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.connector.BitTorrentHandshakeHandler;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.johnnei.javatorrent.network.ByteBufferUtils.getBytes;
import static org.johnnei.javatorrent.network.ByteBufferUtils.getString;
import static org.johnnei.javatorrent.network.ByteBufferUtils.putString;

public class BitTorrentHandshakeHandlerImpl implements BitTorrentHandshakeHandler {

	public static final int HANDSHAKE_SIZE = 68;

	public static final String PROTOCOL_NAME = "BitTorrent protocol";

	public static final int PROTOCOL_LENGTH = PROTOCOL_NAME.length();

	private static final Logger LOGGER = LoggerFactory.getLogger(BitTorrentHandshakeHandlerImpl.class);

	private static final int TORRENT_HASH_OFFSET = 28;

	private final ByteBuffer bittorrentHandshake = ByteBuffer.allocate(HANDSHAKE_SIZE);

	private final TorrentClient torrentClient;

	private final Clock clock;

	private final Selector selector;

	public BitTorrentHandshakeHandlerImpl(TorrentClient torrentClient) {
		this(torrentClient, Clock.systemDefaultZone());
	}

	BitTorrentHandshakeHandlerImpl(TorrentClient torrentClient, Clock clock) {
		this.torrentClient = torrentClient;
		this.clock = clock;

		bittorrentHandshake.put((byte) 0x13);
		putString(bittorrentHandshake, PROTOCOL_NAME);
		bittorrentHandshake.put(torrentClient.getExtensionBytes());
		bittorrentHandshake.position(bittorrentHandshake.position() + 20);
		bittorrentHandshake.put(torrentClient.getPeerId());

		try {
			selector = Selector.open();
		} catch (IOException e) {
			throw new IllegalStateException("NIO is not available", e);
		}

		torrentClient.getExecutorService().scheduleWithFixedDelay(this::pollHandshakesReady, 50, 50, TimeUnit.MILLISECONDS);
	}

	@Override
	public synchronized void onConnectionEstablished(ISocket socket, byte[] torrentHash) {
		try {
			sendHandshake((ByteChannel) socket.getChannel(), torrentHash);
		} catch (IOException e) {
			LOGGER.debug("Failed to send handshake", e);
			close(socket);
			return;
		}
		try {
			HandshakeState state = new HandshakeState(clock, socket, torrentHash);
			socket.getChannel().register(selector, SelectionKey.OP_READ, state);
		} catch (ClosedChannelException e) {
			throw new IllegalStateException("Attempted to connect to peer on closed selector.", e);
		}
	}

	public void close(ISocket socket) {
		try {
			socket.close();
		} catch (IOException ce) {
			LOGGER.warn("Failed to close channel.", ce);
		}
	}

	@Override
	public synchronized void onConnectionReceived(ISocket socket) {
		try {
			HandshakeState state = new HandshakeState(clock, socket,null);
			socket.getChannel().register(selector, SelectionKey.OP_READ, state);
		} catch (ClosedChannelException e) {
			throw new IllegalStateException("Attempted to connect to peer on closed selector.", e);
		}
	}

	public void stop() {
		selector.wakeup();
		try {
			selector.close();
		} catch (IOException e) {
			LOGGER.warn("Error during shutdown of selector", e);
		}
	}

	private void sendHandshake(ByteChannel channel, byte[] torrentHash) throws IOException {
		bittorrentHandshake.position(TORRENT_HASH_OFFSET);
		bittorrentHandshake.put(torrentHash);
		bittorrentHandshake.position(bittorrentHandshake.capacity());
		bittorrentHandshake.flip();

		channel.write(bittorrentHandshake);
		if (bittorrentHandshake.hasRemaining()) {
			throw new IOException("Socket buffer exceeded.");
		}
	}

	private synchronized void pollHandshakesReady() {
		try {
			selector.selectNow();
			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey key = keys.next();

				HandshakeState state = (HandshakeState) key.attachment();
				ByteChannel channel = (ByteChannel) key.channel();
				handlePeer(state, channel);

				keys.remove();
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to select channels.", e);
		}

		// Handles channels which didn't respond within 5 seconds.
		for (SelectionKey key : selector.keys()) {
			HandshakeState state = (HandshakeState) key.attachment();
			ByteChannel channel = (ByteChannel) key.channel();

			if (clock.instant().minusSeconds(5).isAfter(state.getConnectionStart())) {
				LOGGER.debug("Handshake timed out for {} missing {} bytes.", channel, state.getHandshakeBuffer().remaining());
				close(state.getSocket());
			}
		}
	}

	private void handlePeer(HandshakeState state, ByteChannel channel) {
		try {
			channel.read(state.getHandshakeBuffer());

			if (!state.getHandshakeBuffer().hasRemaining()) {
				onHandshakeReceived(channel, state);
			}
		} catch (Exception e) {
			LOGGER.debug("Failed to process peer handshake.", e);
			close(state.getSocket());
		}
	}

	private void onHandshakeReceived(ByteChannel channel, HandshakeState state) throws IOException {
		ByteBuffer buffer = state.getHandshakeBuffer();
		buffer.flip();

		int length = buffer.get();

		if (length != PROTOCOL_LENGTH) {
			throw new BitTorrentProtocolViolationException("Incorrect handshake length");
		}

		String protocol = getString(buffer, 0x13);

		if (!PROTOCOL_NAME.equals(protocol)) {
			throw new BitTorrentProtocolViolationException("Incorrect protocol");
		}

		byte[] extensionBytes = getBytes(buffer, 8);
		byte[] torrentHash = getBytes(buffer, 20);

		Torrent torrent;

		if (state.getExpectedTorrent() == null) {
			torrent = torrentClient.getTorrentByHash(torrentHash)
				.orElseThrow(() -> new BitTorrentProtocolViolationException("Remote peer is downloading torrent we don't have."));
			sendHandshake(channel, torrentHash);
		} else {
			if (!Arrays.equals(state.getExpectedTorrent(), torrentHash)) {
				throw new BitTorrentProtocolViolationException("Remote peer reported different torrent than requested.");
			}
			torrent = torrentClient.getTorrentByHash(state.getExpectedTorrent())
				.orElseThrow(() -> new BitTorrentProtocolViolationException("We requested a torrent from the remote peer which we no longer have."));
		}

		byte[] receivedPeerId = getBytes(buffer,20);

		Peer peer = new Peer.Builder()
			.setId(receivedPeerId)
			.setExtensionBytes(extensionBytes)
			.setTorrent(torrent)
			.setSocket(new BitTorrentSocket(torrentClient.getMessageFactory(), state.getSocket()))
			.build();

		torrent.addPeer(peer);
		// FIXME: The SelectionKey should be cancelled here.
	}

}
