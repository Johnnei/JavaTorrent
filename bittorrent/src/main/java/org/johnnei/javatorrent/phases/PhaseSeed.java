package org.johnnei.javatorrent.phases;

import java.util.Optional;
import java.util.function.BiFunction;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.choking.PermissiveUploadStrategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A phase which is meant to be used for seeding torrents. All peers which are considered seeders (having all pieces) will be disconnected. They aren't useful
 * to us anymore.
 */
public class PhaseSeed implements IDownloadPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseSeed.class);

	private final Torrent torrent;

	private IChokingStrategy chokingStrategy;

	/**
	 * Creates a new seeding phase.
	 * @param torrentClient The client on which this phase operates.
	 * @param torrent The torrent for which this phase applies.
	 *
	 * @see org.johnnei.javatorrent.phases.PhaseRegulator.Builder#registerPhase(Class, BiFunction, Optional)
	 */
	public PhaseSeed(TorrentClient torrentClient, Torrent torrent) {
		this.torrent = torrent;
		chokingStrategy = new PermissiveUploadStrategy();
	}

	@Override
	public boolean isDone() {
		return false;
	}

	@Override
	public void process() {
		torrent.getPeers().stream()
				.filter(p -> p.countHavePieces() == torrent.getFileSet().getPieceCount())
				.forEach(p -> p.getBitTorrentSocket().close());
	}

	@Override
	public void onPhaseEnter() {
		// Nothing to do upon entering this phase.
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
