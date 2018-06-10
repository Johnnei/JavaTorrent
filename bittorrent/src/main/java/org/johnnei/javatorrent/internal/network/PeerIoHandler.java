package org.johnnei.javatorrent.internal.network;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentProtocolViolationException;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * Class which handles the processing of IO on {@link org.johnnei.javatorrent.network.socket.ISocket}.
 */
public class PeerIoHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerIoHandler.class);

	private final ScheduledFuture<?> task;

	private final Selector selector;

	public PeerIoHandler(ScheduledExecutorService scheduledExecutorService) {
		try {
			selector = Selector.open();
		} catch (IOException e) {
			throw new IllegalStateException("Failed to create async selector.", e);
		}

		task = scheduledExecutorService.scheduleWithFixedDelay(this::pollChannels, 50, 50, TimeUnit.MILLISECONDS);
	}

	public void registerPeer(Peer peer, ISocket socket) {
		try {
			if ((socket.getReadableChannel().validOps() & SelectionKey.OP_WRITE) != 0) {
				socket.getReadableChannel().register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, peer);
			} else {
				socket.getReadableChannel().register(selector, SelectionKey.OP_READ, peer);
				socket.getWritableChannel().register(selector, SelectionKey.OP_WRITE, peer);
			}
		} catch (ClosedChannelException e) {
			throw new IllegalStateException("Channel mustn't be closed to be handled.", e);
		}
	}

	public void shutdown() {
		task.cancel(false);
	}

	public void pollChannels() {
		try {
			selector.selectNow();

			Iterator<SelectionKey> keys = selector.selectedKeys().iterator();
			while (keys.hasNext()) {
				SelectionKey key = keys.next();
				Peer peer = (Peer) key.attachment();

				handlePeer(key, peer);

				keys.remove();
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to process ready channels.", e);
		}
	}

	public void handlePeer(SelectionKey key, Peer peer) {
		try (MDC.MDCCloseable ignored = MDC.putCloseable("context", peer.getIdAsString())) {
			BitTorrentSocket socket = peer.getBitTorrentSocket();

			if (key.isReadable()) {
				onDataAvailable(peer, socket);
			}
			if (key.isWritable()) {
				onDataRequested(peer, socket);
			}
		}
	}

	private void onDataAvailable(Peer peer, BitTorrentSocket socket) {
		try {
			while (socket.canReadMessage()) {
				IMessage message = socket.readMessage();
				message.process(peer);
			}
		} catch (BitTorrentProtocolViolationException e) {
			LOGGER.debug("Peer {} violated protocol.", peer, e);
			socket.close();
		} catch (IOException e) {
			LOGGER.warn("Failed to process peer {}", peer, e);
		}
	}

	private void onDataRequested(Peer peer, BitTorrentSocket socket) {
		try {
			if (socket.hasOutboundMessages()) {
				socket.sendMessages();
			} else {
				peer.queueNextPieceForSending();
			}
		} catch (IOException e) {
			LOGGER.warn("Failed to process peer {}", peer, e);
		}
	}

}
