package org.johnnei.javatorrent.torrent.download;

import java.io.IOException;

import org.johnnei.javatorrent.torrent.TorrentManager;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.download.peer.PeerDirection;
import org.johnnei.javatorrent.torrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.utils.ThreadUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeersWriterRunnable implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(PeersWriterRunnable.class);

	private TorrentManager manager;

	public PeersWriterRunnable(TorrentManager manager) {
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

		try {
			BitTorrentSocket socket = peer.getBitTorrentSocket();

			if (socket.canWriteMessage()) {
				peer.getBitTorrentSocket().sendMessage();
			} else if (peer.getWorkQueueSize(PeerDirection.Upload) > 0) {
				peer.queueNextPieceForSending();
			}
		} catch (IOException e) {
			LOGGER.error(String.format("IO Error for peer: %s", peer), e);
			peer.getBitTorrentSocket().close();
		}
	}
}
