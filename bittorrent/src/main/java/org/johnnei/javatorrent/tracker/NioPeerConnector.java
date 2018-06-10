package org.johnnei.javatorrent.tracker;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.time.Clock;
import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.network.connector.PeerConnectionState;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.torrent.Torrent;

/**
 * A peer connector which allows simultaneously connect to several peers at a time.
 * This implementation is optimized to be non-blocking thus allowing it to use the {@link TorrentClient#getExecutorService()} instead of a dedicated thread.
 */
public class NioPeerConnector implements IPeerConnector {

	private static final Logger LOGGER = LoggerFactory.getLogger(NioPeerConnector.class);

	private final Clock clock;

	private final TorrentClient torrentClient;

	private final int maxConcurrentConnecting;

	private final Queue<PeerConnectionState> connectQueue;

	private final Selector connected;

	private ScheduledFuture<?> pollTask;

	/**
	 * Creates a new unstarted peer connector.
	 * @param torrentClient The client for which this connector will connect peers.
	 * @param maxConcurrentConnecting The maximum amount of pending connections at any given time.
	 */
	public NioPeerConnector(TorrentClient torrentClient, int maxConcurrentConnecting) {
		this(Clock.systemDefaultZone(), torrentClient, maxConcurrentConnecting);
	}

	NioPeerConnector(Clock clock, TorrentClient torrentClient, int maxConcurrentConnecting) {
		this.clock = clock;
		this.torrentClient = torrentClient;
		this.maxConcurrentConnecting = maxConcurrentConnecting;
		connectQueue = new ConcurrentLinkedQueue<>();
		try {
			connected = Selector.open();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to create NIO selector", e);
		}
	}

	@Override
	public void enqueuePeer(PeerConnectInfo peer) {
		if (peer == null) {
			return;
		}

		LOGGER.debug("Enqueued {} for connecting.", peer);
		connectQueue.add(new PeerConnectionState(peer));
	}

	@Override
	public void start() {
		pollTask = torrentClient.getExecutorService().scheduleWithFixedDelay(this::pollReadyConnections, 50, 50, TimeUnit.MILLISECONDS);
	}

	@Override
	public void stop() {
		pollTask.cancel(false);
	}

	void pollReadyConnections() {
		try {
			updateReadyConnections();
			degradeTimedOutConnections();
			enqueueNewConnections();
		} catch (Exception e) {
			LOGGER.warn("Peer connector update failed", e);
		}
	}

	private void enqueueNewConnections() {
		int available = maxConcurrentConnecting - getConnectingCount();
		PeerConnectionState state;
		while (available > 0 && (state = connectQueue.poll()) != null) {
			degradeSocket(state, torrentClient.getConnectionDegradation().createPreferredSocket());
			available--;
		}
	}

	private void degradeTimedOutConnections() {
		for (SelectionKey key : connected.keys()) {
			PeerConnectionState state = (PeerConnectionState) key.attachment();

			if (clock.instant().minusSeconds(10).isAfter(state.getStartTime())) {
				onDegradeSocket(state);
			}
		}
	}

	private void onDegradeSocket(PeerConnectionState state) {
		Optional<ISocket> socket = torrentClient.getConnectionDegradation().degradeSocket(state.getCurrentSocket());
		try {
			state.getCurrentSocket().close();
		} catch (IOException e) {
			LOGGER.debug("Failed to close channel.", e);
		}

		degradeSocket(state, socket.orElse(null));
	}

	private void degradeSocket(PeerConnectionState state, ISocket socket) {
		if (socket != null) {
			try {
				state.updateSocket(clock.instant(), socket);

				if ((socket.getReadableChannel().validOps() & SelectionKey.OP_CONNECT) != 0) {
					socket.getReadableChannel().register(connected, SelectionKey.OP_CONNECT, state);
					LOGGER.debug("Connecting to {} with socket type {}", state.getPeer().getAddress(), socket.getClass().getSimpleName());
					socket.connect(state.getPeer().getAddress());
				} else {
					LOGGER.debug("Socket type is connectionless, passing directly to handshake handler.");
					socket.connect(state.getPeer().getAddress());
					onConnected(state);
				}
			} catch (IOException e) {
				LOGGER.warn("Failed to start up connection", e);
			}
		} else {
			LOGGER.debug("Failed to establish connection with {}", state.getPeer().getAddress());
		}
	}

	private void updateReadyConnections() {
		try {
			connected.selectNow();
			Iterator<SelectionKey> keys = connected.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey key = keys.next();
				PeerConnectionState state = (PeerConnectionState) key.attachment();

				if (state.getCurrentSocket().isConnected()) {
					onConnected(state);
					key.cancel();
				} else {
					onDegradeSocket(state);
				}

				keys.remove();
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to establish connections to new peers", e);
		}
	}

	private void onConnected(PeerConnectionState state) {
		torrentClient.getHandshakeHandler()
			.onConnectionEstablished(state.getCurrentSocket(), state.getPeer().getTorrent().getMetadata().getHash());
	}

	@Override
	public int getConnectingCount() {
		return connected.keys().size();
	}

	@Override
	public int getConnectingCountFor(Torrent torrent) {
		return (int) connected.keys().stream()
			.map(key -> (PeerConnectionState) key.attachment())
			.filter(state -> state.getPeer().getTorrent().equals(torrent))
			.count();
	}

}
