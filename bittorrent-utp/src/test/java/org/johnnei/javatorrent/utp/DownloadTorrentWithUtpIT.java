package org.johnnei.javatorrent.utp;

import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.phases.PhaseData;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.DownloadTorrentIT;
import org.johnnei.javatorrent.tracker.PeerConnector;
import org.johnnei.javatorrent.tracker.UncappedDistributor;

/**
 * Tests the ability to cleanly download a torrent.
 */
public class DownloadTorrentWithUtpIT extends DownloadTorrentIT {

	protected TorrentClient createTorrentClient(CountDownLatch latch) throws Exception {
		UtpModule utpModule = new UtpModule();

		return new TorrentClient.Builder()
				// Disable incoming TCP connections, only expect UTP
				.acceptIncomingConnections(false)
				.registerModule(utpModule)
				.setConnectionDegradation(new ConnectionDegradation.Builder()
						.registerDefaultConnectionType(utpModule.getUtpSocketClass(), utpModule.createSocketFactory())
						.build())
				.setDownloadPort(DummyEntity.findAvailableTcpPort())
				.setExecutorService(Executors.newScheduledThreadPool(2))
				.setPeerConnector(PeerConnector::new)
				.setPeerDistributor(UncappedDistributor::new)
				.registerTrackerProtocol("stub", (s, torrentClient) -> null)
				.setPhaseRegulator(new PhaseRegulator.Builder()
						.registerInitialPhase(PhaseData.class, PhaseData::new, Optional.of(PhaseSeedCountdown.class))
						.registerPhase(PhaseSeedCountdown.class, ((torrentClient, torrent) -> new PhaseSeedCountdown(latch, torrentClient, torrent)),
								Optional.empty())
						.build()
				).build();
	}

}
