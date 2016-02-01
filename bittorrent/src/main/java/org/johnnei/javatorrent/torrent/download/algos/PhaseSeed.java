package org.johnnei.javatorrent.torrent.download.algos;

import java.util.Collection;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhaseSeed implements IDownloadPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseSeed.class);

	private TorrentClient torrentClient;

	private Torrent torrent;

	public PhaseSeed(TorrentClient torrentClient, Torrent torrent) {
		this.torrentClient = torrentClient;
		this.torrent = torrent;
	}

	@Override
	public boolean isDone() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void process() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPhaseEnter() {
		// TODO Auto-generated method stub

	}

	@Override
	public void onPhaseExit() {
		LOGGER.info("Upload target reached");
	}

	@Override
	public Collection<Peer> getRelevantPeers(Collection<Peer> peers) {
		return null;
	}

}
