package org.johnnei.javatorrent.phases;

import java.io.File;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.algos.choking.PermissiveStrategy;
import org.johnnei.javatorrent.torrent.algos.pieceselector.AvailabilityPrioritizer;
import org.johnnei.javatorrent.torrent.algos.pieceselector.PiecePrioritizer;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * The download phase in which the actual torrent files will be downloaded.
 */
public class PhaseData implements IDownloadPhase {

	private static final Logger LOGGER = LoggerFactory.getLogger(PhaseData.class);

	private final Torrent torrent;

	private final TorrentClient torrentClient;

	private final IChokingStrategy chokingStrategy;

	private final PiecePrioritizer piecePrioritizer;

	/**
	 * Creates a new Data Phase for the given torrent.
	 * @param torrentClient The client used to notify trackers.
	 * @param torrent The torrent which we are downloading.
	 */
	public PhaseData(TorrentClient torrentClient, Torrent torrent) {
		this.torrentClient = torrentClient;
		this.torrent = torrent;
		chokingStrategy = new PermissiveStrategy();
		piecePrioritizer = new AvailabilityPrioritizer();
	}

	@Override
	public boolean isDone() {
		return torrent.getFileSet().isDone();
	}

	@Override
	public void process() {
	}

	@Override
	public void onPhaseEnter() {
		torrent.checkProgress();
		File downloadFolder = torrent.getFileSet().getDownloadFolder();

		if (!downloadFolder.exists() && !downloadFolder.mkdirs()) {
			throw new TorrentException(String.format("Failed to create download folder: %s", downloadFolder.getAbsolutePath()));
		}
	}

	@Override
	public void onPhaseExit() {
		torrentClient.getTrackersFor(torrent).forEach(tracker -> tracker.getInfo(torrent).get().setEvent(TrackerEvent.EVENT_COMPLETED));
		LOGGER.info("Download of {} completed", torrent);
	}

	@Override
	public IChokingStrategy getChokingStrategy() {
		return chokingStrategy;
	}

	@Override
	public boolean isPeerSupportedForDownload(Peer peer) {
		return true;
	}

	@Override
	public Optional<AbstractFileSet> getFileSet() {
		return Optional.of(torrent.getFileSet());
	}

	@Override
	public PiecePrioritizer getPiecePrioritizer() {
		return piecePrioritizer;
	}
}
