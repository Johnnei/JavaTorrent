package org.johnnei.javatorrent.utp;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Disabled;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.it.EndToEndDownload;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.phases.PhaseData;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.algos.requests.RateBasedLimiter;
import org.johnnei.javatorrent.tracker.NioPeerConnector;
import org.johnnei.javatorrent.tracker.UncappedDistributor;

/**
 * Tests the ability to cleanly download a torrent.
 */
@Disabled("JBT-59 has significantly improved the stability of uTP, it is not fast enough yet to actually pass this test within reasonable time.")
public class DownloadTorrentWithUtpIT extends EndToEndDownload {

	public DownloadTorrentWithUtpIT() {
	}

	protected TorrentClient.Builder createTorrentClient(CountDownLatch latch) throws Exception {
		int port = DummyEntity.findAvailableUdpPort();
		UtpModule utpModule = new UtpModule.Builder().build();

		return new TorrentClient.Builder()
				// Disable incoming TCP connections, only expect UTP
				.acceptIncomingConnections(false)
				.registerModule(utpModule)
				.setConnectionDegradation(new ConnectionDegradation.Builder()
						.registerDefaultConnectionType(utpModule.getUtpSocketClass(), utpModule.createSocketFactory())
						.build())
				.setRequestLimiter(new RateBasedLimiter())
				.setDownloadPort(port)
				.setPeerConnector(tc -> new NioPeerConnector(tc, 4))
				.setPeerDistributor(UncappedDistributor::new)
				.registerTrackerProtocol("stub", (s, torrentClient) -> null)
				.setPhaseRegulator(new PhaseRegulator.Builder()
						.registerInitialPhase(PhaseData.class, PhaseData::new, PhaseSeedCountdown.class)
						.registerPhase(PhaseSeedCountdown.class, ((torrentClient, torrent) -> new PhaseSeedCountdown(latch, torrentClient, torrent)))
						.build()
				);
	}

}
