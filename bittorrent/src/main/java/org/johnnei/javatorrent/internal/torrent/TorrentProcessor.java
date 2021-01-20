package org.johnnei.javatorrent.internal.torrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.torrent.selection.PieceSelectionHandler;
import org.johnnei.javatorrent.internal.torrent.selection.PieceSelectionState;
import org.johnnei.javatorrent.internal.tracker.TrackerManager;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.PeerStateAccess;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A state machine wrapped around the torrent
 */
class TorrentProcessor implements PeerStateAccess {

	private static final Logger LOGGER = LoggerFactory.getLogger(TorrentProcessor.class);

	private final TorrentManager torrentManager;

	private final TorrentClient torrentClient;

	private final TrackerManager trackerManager;

	private final Torrent torrent;

	private IDownloadPhase downloadPhase;

	private PieceSelectionHandler pieceSelectionHandler;

	private Collection<ScheduledFuture<?>> scheduledTasks;

	public TorrentProcessor(TorrentManager torrentManager, TrackerManager trackerManager, TorrentClient torrentClient, Torrent torrent) {
		this.torrentManager = torrentManager;
		this.trackerManager = trackerManager;
		this.torrentClient = torrentClient;
		this.torrent = torrent;
		scheduledTasks = new ArrayList<>(3);

		downloadPhase = torrentClient.getPhaseRegulator().createInitialPhase(torrentClient, torrent);
		doPhaseEnter();

		scheduledTasks.add(torrentClient.getExecutorService().scheduleAtFixedRate(this::updateTorrentState, 0, 250, TimeUnit.MILLISECONDS));
		scheduledTasks.add(torrentClient.getExecutorService().scheduleAtFixedRate(this::updateChokingStates, 1, 10, TimeUnit.SECONDS));
		scheduledTasks.add(torrentClient.getExecutorService().scheduleAtFixedRate(this::removeDisconnectedPeers, 30, 60, TimeUnit.SECONDS));
		scheduledTasks.add(torrentClient.getExecutorService().scheduleAtFixedRate(this::updateTrackerStates, 10, 30, TimeUnit.SECONDS));
	}

	public void updateTrackerStates() {
		trackerManager.announce(torrent);
	}

	public void removeDisconnectedPeers() {
		torrent.getPeers().stream().
				filter(p -> p.getBitTorrentSocket().closed()).
				forEach(torrent::removePeer);
	}

	public void updateChokingStates() {
		torrent.getPeers().forEach(downloadPhase.getChokingStrategy()::updateChoking);
	}

	public void updateTorrentState() {
		try {
			if (downloadPhase.isDone()) {
				downloadPhase.onPhaseExit();
				Optional<IDownloadPhase> newPhase = torrentClient.getPhaseRegulator().createNextPhase(downloadPhase, torrentClient, torrent);

				if (newPhase.isPresent()) {
					LOGGER.info("Torrent transitioning from {} to {}", downloadPhase, newPhase.get());
					downloadPhase = newPhase.get();
					doPhaseEnter();
				} else {
					LOGGER.info("Torrent ended from {}", downloadPhase);
					shutdownTorrent();
					return;
				}
			}

			downloadPhase.process();
			pieceSelectionHandler.updateState();
		} catch (Exception e) {
			LOGGER.error("Failed to update torrent state", e);
			shutdownTorrent();
		}
	}

	public void shutdownTorrent() {
		for (ScheduledFuture<?> task : scheduledTasks) {
			task.cancel(false);
		}

		torrentManager.removeTorrent(torrent);
	}

	@Override
	public int getPendingBlocks(Peer peer, PeerDirection direction) {
		if (direction == PeerDirection.Upload) {
			return peer.getWorkQueueSize(PeerDirection.Upload);
		} else {
			return pieceSelectionHandler.getBlockQueueFor(peer);
		}
	}

	private void doPhaseEnter() {
		downloadPhase.onPhaseEnter();
		Supplier<Optional<AbstractFileSet>> fileSetSupplier = () -> downloadPhase.getFileSet();
		pieceSelectionHandler = new PieceSelectionHandler(
			fileSetSupplier,
			downloadPhase.getPiecePrioritizer(),
			new PieceSelectionState(torrent, downloadPhase::isPeerSupportedForDownload, fileSetSupplier)
		);
	}

}
