package org.johnnei.javatorrent.internal.torrent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.torrent.Torrent;

/**
 * A state machine wrapped around the torrent
 */
class TorrentProcessor {

	private final TorrentManager torrentManager;

	private final TorrentClient torrentClient;

	private final Torrent torrent;

	private IDownloadPhase downloadPhase;

	private Collection<ScheduledFuture<?>> scheduledTasks;

	public TorrentProcessor(TorrentManager torrentManager, TorrentClient torrentClient, Torrent torrent) {
		this.torrentManager = torrentManager;
		this.torrentClient = torrentClient;
		this.torrent = torrent;
		scheduledTasks = new ArrayList<>(3);

		downloadPhase = torrentClient.getPhaseRegulator().createInitialPhase(torrentClient, torrent);
		downloadPhase.onPhaseEnter();

		scheduledTasks.add(torrentClient.getExecutorService().scheduleAtFixedRate(this::updateTorrentState, 0, 250, TimeUnit.MILLISECONDS));
		scheduledTasks.add(torrentClient.getExecutorService().scheduleAtFixedRate(this::updateChokingStates, 1, 10, TimeUnit.SECONDS));
		scheduledTasks.add(torrentClient.getExecutorService().scheduleAtFixedRate(this::removeDisconnectedPeers, 30, 60, TimeUnit.SECONDS));
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
		if (downloadPhase.isDone()) {
			downloadPhase.onPhaseExit();
			Optional<IDownloadPhase> newPhase = torrentClient.getPhaseRegulator().createNextPhase(downloadPhase, torrentClient, torrent);

			if (newPhase.isPresent()) {
				downloadPhase = newPhase.get();
				downloadPhase.onPhaseEnter();
			} else {
				shutdownTorrent();
				return;
			}
		}

		downloadPhase.process();
	}

	public void shutdownTorrent() {
		for (ScheduledFuture<?> task : scheduledTasks) {
			task.cancel(false);
		}

		torrentManager.removeTorrent(torrent);
	}

}
