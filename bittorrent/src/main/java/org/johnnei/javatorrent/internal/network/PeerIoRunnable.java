package org.johnnei.javatorrent.internal.network;

import java.io.IOException;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.internal.torrent.TorrentManager;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

		try {
			handleWrite(peer);
			handleRead(peer);
		} catch (IOException e) {
			LOGGER.error(String.format("IO Error for peer: %s", peer), e);
			peer.getBitTorrentSocket().close();
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
