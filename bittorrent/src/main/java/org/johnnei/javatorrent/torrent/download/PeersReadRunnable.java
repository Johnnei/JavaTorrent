package org.johnnei.javatorrent.torrent.download;

import java.io.IOException;

import org.johnnei.javatorrent.network.protocol.IMessage;
import org.johnnei.javatorrent.torrent.TorrentManager;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeersReadRunnable implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeersReadRunnable.class);

	private TorrentManager manager;

	public PeersReadRunnable(TorrentManager manager) {
		this.manager = manager;
	}

	@Override
	public void run() {
		while (true) {
			for (Torrent torrent : manager.getTorrents()) {
				processTorrent(torrent);
			}
			ThreadUtils.sleep(1);
		}
	}

	private void processTorrent(Torrent torrent) {
		synchronized (torrent) {
			for (Peer peer : torrent.getPeers()) {
				processPeer(peer);
			}
		}
	}

	private void processPeer(Peer peer) {
		if (peer.getBitTorrentSocket().closed()) {
			return;
		}

		BitTorrentSocket socket = peer.getBitTorrentSocket();

		try {
			if (!socket.canReadMessage()) {
				return;
			}

			IMessage message = socket.readMessage();
			message.process(peer);
			peer.updateLastActivity();
		} catch (IOException e) {
			LOGGER.error("Caught IO exception in peer connection, closing socket.", e);
			peer.getBitTorrentSocket().close();
		} catch (Exception e) {
			LOGGER.warn("Caught non-fatal exception.", e);
		}
	}

}
