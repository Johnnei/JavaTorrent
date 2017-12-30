package org.johnnei.javatorrent.torrent;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.it.EndToEndDownload;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.network.socket.NioTcpSocket;
import org.johnnei.javatorrent.phases.PhaseData;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.algos.requests.RateBasedLimiter;
import org.johnnei.javatorrent.tracker.NioPeerConnector;
import org.johnnei.javatorrent.tracker.UncappedDistributor;

/**
 * Tests the ability to cleanly download a torrent.
 */
public class DownloadTorrentIT extends EndToEndDownload {

    protected TorrentClient createTorrentClient(CountDownLatch latch) throws Exception {
        return new TorrentClient.Builder()
            .acceptIncomingConnections(true)
            .setConnectionDegradation(new ConnectionDegradation.Builder()
                .registerDefaultConnectionType(NioTcpSocket.class, NioTcpSocket::new)
                .build())
            .setDownloadPort(DummyEntity.findAvailableTcpPort())
            .setExecutorService(Executors.newScheduledThreadPool(8))
            .setPeerConnector(tc -> new NioPeerConnector(tc, 4))
            .setRequestLimiter(new RateBasedLimiter())
            .setPeerDistributor(UncappedDistributor::new)
            .registerTrackerProtocol("stub", (s, torrentClient) -> null)
            .setPhaseRegulator(new PhaseRegulator.Builder()
                .registerInitialPhase(PhaseData.class, PhaseData::new, PhaseSeedCountdown.class)
                .registerPhase(
                    PhaseSeedCountdown.class,
                    ((torrentClient, torrent) -> new PhaseSeedCountdown(latch, torrentClient, torrent))
                )
                .build()
            ).build();
    }
}
