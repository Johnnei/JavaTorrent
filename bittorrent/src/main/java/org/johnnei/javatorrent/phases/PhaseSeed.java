package org.johnnei.javatorrent.phases;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.choking.PermissiveStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PhaseSeed implements IDownloadPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseSeed.class);

	private final TorrentClient torrentClient;

	private final Torrent torrent;

	private IChokingStrategy chokingStrategy;

	public PhaseSeed(TorrentClient torrentClient, Torrent torrent) {
		this.torrentClient = torrentClient;
		this.torrent = torrent;
		chokingStrategy = new PermissiveStrategy();
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
	public IChokingStrategy getChokingStrategy() {
		return chokingStrategy;
	}
}
