package org.johnnei.javatorrent.download.tracker;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.johnnei.javatorrent.test.DummyEntity.createTorrent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.test.Whitebox;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.tracker.TorrentInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class UdpTrackerTest extends EasyMockSupport {

	private UdpTracker cut;

	private TorrentClient torrentClientMock;

	@Mock
	private TrackerConnection trackerConnectionMock;

	private TestClock clock;

	private Clock fixedClock;

	@Before
	public void setUp() {
		fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		clock = new TestClock(fixedClock);
		torrentClientMock = StubEntity.stubTorrentClient(this);
		cut = new UdpTracker("udp://localhost:80", torrentClientMock, clock);
		Whitebox.setInternalState(cut, "connection", trackerConnectionMock);
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
	public void testGetInfo() {
		replayAll();

		Torrent torrentOne = createTorrent(torrentClientMock);
		Torrent torrentTwo = createTorrent(torrentClientMock);
		Torrent torrentThree = createTorrent(torrentClientMock);

		cut.addTorrent(torrentOne);
		cut.addTorrent(torrentTwo);

		TorrentInfo torrentInfoOne = cut.getInfo(torrentOne).orElseThrow(() -> new AssertionError("Null on existing torrent"));
		assertEquals("Torrent info did not match torrent", torrentOne, torrentInfoOne.getTorrent());

		TorrentInfo torrentInfoTwo = cut.getInfo(torrentTwo).orElseThrow(() -> new AssertionError("Null on existing torrent"));
		assertEquals("Torrent info did not match torrent", torrentTwo, torrentInfoTwo.getTorrent());

		Assert.assertEquals("Torrent info returned for not registered torrent", Optional.empty(), cut.getInfo(torrentThree));
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
