package org.johnnei.javatorrent.download.tracker;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.johnnei.javatorrent.test.DummyEntity.createTorrent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.test.ExecutorServiceMock;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.test.Whitebox;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.algos.IDownloadPhase;
import org.johnnei.javatorrent.torrent.download.algos.IPeerManager;
import org.johnnei.javatorrent.torrent.download.tracker.IPeerConnector;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class UdpTrackerTest extends EasyMockSupport {

	private UdpTracker cut;

	@Mock
	private TorrentClient torrentClientMock;

	@Mock
	private IPeerConnector peerConnectorMock;

	@Mock
	private IPeerManager peerManagerMock;

	@Mock
	private PhaseRegulator phaseRegulatorMock;

	@Mock
	private IDownloadPhase downloadPhaseMock;

	@Mock
	private TrackerConnection trackerConnectionMock;

	private TestClock clock;

	private Clock fixedClock;

	@Before
	public void setUp() {
		fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		clock = new TestClock(fixedClock);
		cut = new UdpTracker("udp://localhost:80", torrentClientMock, clock);
		Whitebox.setInternalState(cut, "connection", trackerConnectionMock);

		ExecutorService service = new ExecutorServiceMock();

		// Setup getters
		expect(torrentClientMock.getPeerConnector()).andStubReturn(peerConnectorMock);
		expect(torrentClientMock.getExecutorService()).andStubReturn(service);
		expect(torrentClientMock.getPeerManager()).andStubReturn(peerManagerMock);
		expect(torrentClientMock.getPhaseRegulator()).andStubReturn(phaseRegulatorMock);
		expect(phaseRegulatorMock.createInitialPhase(eq(torrentClientMock), notNull())).andReturn(downloadPhaseMock);
	}

	@Test
	public void testAnnounce() throws Exception {
		final int interval = 12345;
		expect(trackerConnectionMock.announce(EasyMock.notNull())).andReturn(interval);

		replayAll();

		Torrent torrent = createTorrent(torrentClientMock);

		cut.addTorrent(torrent);
		cut.announce(torrent);

		verifyAll();

		assertEquals("Announce interval got ignored.", (Integer) interval, Whitebox.<Integer>getInternalState(cut, "announceInterval"));
		assertEquals("Error count got increased on succes", 0, cut.getErrorCount());
	}

	@Test
	public void testAnnounceWithinInterval() throws Exception {
		replayAll();

		Torrent torrent = createTorrent(torrentClientMock);

		cut.addTorrent(torrent);

		// Move clock back to simulate that we're still in the interval period
		clock.setClock(Clock.offset(fixedClock, Duration.ofSeconds(-10)));

		cut.announce(torrent);

		verifyAll();

		assertEquals("Error count got increased on succes", 0, cut.getErrorCount());
	}

	@Test
	public void testScrape() throws Exception {
		trackerConnectionMock.scrape(EasyMock.notNull());
		expectLastCall();

		replayAll();

		Torrent torrent = createTorrent(torrentClientMock);

		cut.addTorrent(torrent);
		cut.scrape();

		verifyAll();

		assertEquals("Error count got increased on succes", 0, cut.getErrorCount());
	}

	@Test
	public void testScrapeWithinInterval() {
		replayAll();
		Torrent torrent = createTorrent(torrentClientMock);

		// Move clock back to simulate that we're still in the interval period
		clock.setClock(Clock.offset(fixedClock, Duration.ofSeconds(-10)));

		cut.addTorrent(torrent);
		cut.scrape();

		verifyAll();
	}

	@Ignore("Feature not yet supported")
	@Test
	public void testScrapeMultiTorrent() {
		fail("Not yet implemented");
	}

	/**
	 * Tests a scrape with 75 torrents which 1 above the protocol limit.
	 */
	@Ignore("Feature not yet supported")
	@Test
	public void testScrapeMultiTorrentLimit() {
		fail("Not yet implemented");
	}

}
