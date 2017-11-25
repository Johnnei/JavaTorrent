package org.johnnei.javatorrent.internal.network;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.internal.torrent.TorrentManager;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

public class PeerIoRunnable implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeerIoRunnable.class);

	private TorrentManager manager;

	public PeerIoRunnable(TorrentManager manager) {
		this.manager = manager;
	}

	@Override
	public void run() {
		manager.getTorrents().forEach(this::processTorrent);
	}

	private void processTorrent(final Torrent torrent) {
		torrent.getPeers().forEach(this::processPeer);
	}

	private void processPeer(Peer peer) {
		if (peer.getBitTorrentSocket().closed()) {
			return;
		}

		try (MDC.MDCCloseable ignored = MDC.putCloseable("context", peer.getIdAsString())) {
			handleWrite(peer);
			handleRead(peer);
		} catch (Exception e) {
			LOGGER.error("Error for peer: {}", peer, e);
			peer.getBitTorrentSocket().close();
			peer.getTorrent().removePeer(peer);
		}
	}

	private void handleWrite(Peer peer) throws IOException {
		BitTorrentSocket socket = peer.getBitTorrentSocket();

		if (socket.hasOutboundMessages()) {
			peer.getBitTorrentSocket().sendMessage();
		} else if (peer.getWorkQueueSize(PeerDirection.Upload) > 0) {
			peer.queueNextPieceForSending();
		}
	}

	private void handleRead(Peer peer) throws IOException {
		BitTorrentSocket socket = peer.getBitTorrentSocket();
		if (!socket.canReadMessage()) {
			return;
		}

		IMessage message = socket.readMessage();
		message.process(peer);
	}
}
