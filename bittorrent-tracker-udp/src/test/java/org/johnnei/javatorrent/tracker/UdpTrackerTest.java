package org.johnnei.javatorrent.tracker;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.same;
import static org.johnnei.javatorrent.test.DummyEntity.createTorrent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.torrent.tracker.TrackerAction;
import org.johnnei.javatorrent.torrent.tracker.TrackerException;
import org.johnnei.javatorrent.torrent.tracker.TrackerManager;
import org.johnnei.javatorrent.tracker.udp.IUdpTrackerPayload;
import org.johnnei.javatorrent.tracker.udp.UdpTrackerSocket;
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
	private UdpTrackerSocket udpTrackerSocketMock;

	private TestClock clock;

	private Clock fixedClock;

	@Before
	public void setUp() throws Exception {
		fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		clock = new TestClock(fixedClock);
		torrentClientMock = createMock(TorrentClient.class);
		cut = new UdpTracker.Builder()
				.setTorrentClient(torrentClientMock)
				.setSocket(udpTrackerSocketMock)
				.setUrl("udp://localhost:80")
				.setClock(clock)
				.build();
	}

	@Test(expected=TrackerException.class)
	public void testUdpTrackerConstructorWithIncorrectProtocol() throws Exception {
		new UdpTracker.Builder()
			.setUrl("http://localhost:80")
			.build();
	}

	@Test
	public void testUdpTrackerConstructorWithIncorrectDomain() throws Exception {
		UdpTracker tracker = new UdpTracker.Builder()
			.setUrl("udp://127.0.0.0.1:80")
			.build();

		assertEquals("Incorrect name", "Unknown", tracker.getName());
		assertEquals("Incorrect state", "Invalid tracker", tracker.getStatus());
	}

	@Test
	public void testAddAndHasTorrent() {
		Torrent torrent = createTorrent();

		replayAll();

		assertFalse("Has torrent before adding", cut.hasTorrent(torrent));
		cut.addTorrent(torrent);
		assertTrue("Doesn't have torrent after adding", cut.hasTorrent(torrent));

		TorrentInfo info = cut.getInfo(torrent).get();
		info.setInfo(5, 6);

		cut.addTorrent(torrent);

		info = cut.getInfo(torrent).get();
		assertEquals("Incorrect seeder count (Torrent info got overwritten)", 5, info.getSeeders());
		assertEquals("Incorrect leechers count (Torrent info got overwritten)", 6, info.getLeechers());
	}

	@Test
	public void testAnnounce() throws Exception {
		Capture<IUdpTrackerPayload> payloadCapture = newCapture();
		udpTrackerSocketMock.submitRequest(same(cut), and(capture(payloadCapture), notNull()));

		TrackerManager trackerManagerMock = createMock(TrackerManager.class);
		expect(torrentClientMock.getTrackerManager()).andStubReturn(trackerManagerMock);
		expect(torrentClientMock.getDownloadPort()).andStubReturn(27960);
		expect(trackerManagerMock.getPeerId()).andReturn(DummyEntity.createPeerId());

		replayAll();

		Torrent torrent = createTorrent();

		cut.addTorrent(torrent);
		cut.announce(torrent);

		verifyAll();
		assertEquals("Incorrect Tracker request type", TrackerAction.ANNOUNCE, payloadCapture.getValue().getAction());
	}

	@Test
	public void testAnnounceWithinInterval() throws Exception {
		replayAll();

		Torrent torrent = createTorrent();

		cut.addTorrent(torrent);
		// Move clock back to simulate that we're still in the interval period
		clock.setClock(Clock.offset(fixedClock, Duration.ofSeconds(-10)));
		cut.announce(torrent);

		verifyAll();
	}


	@Test
	public void testGetInfo() {
		replayAll();

		Torrent torrentOne = createTorrent();
		Torrent torrentTwo = createTorrent();
		Torrent torrentThree = createTorrent();

		cut.addTorrent(torrentOne);
		cut.addTorrent(torrentTwo);

		TorrentInfo torrentInfoOne = cut.getInfo(torrentOne).orElseThrow(() -> new AssertionError("Null on existing torrent"));
		assertEquals("Torrent info did not match torrent", torrentOne, torrentInfoOne.getTorrent());

		TorrentInfo torrentInfoTwo = cut.getInfo(torrentTwo).orElseThrow(() -> new AssertionError("Null on existing torrent"));
		assertEquals("Torrent info did not match torrent", torrentTwo, torrentInfoTwo.getTorrent());

		Assert.assertEquals("Torrent info returned for not registered torrent", Optional.empty(), cut.getInfo(torrentThree));
	}

	@Test
	public void testScrape() throws Exception {
		Capture<IUdpTrackerPayload> payloadCapture = newCapture();
		udpTrackerSocketMock.submitRequest(same(cut), and(capture(payloadCapture), notNull()));
		replayAll();

		Torrent torrent = createTorrent();

		cut.addTorrent(torrent);
		cut.scrape();

		verifyAll();
		assertEquals("Incorrect Tracker request type", TrackerAction.SCRAPE, payloadCapture.getValue().getAction());
	}

	@Test
	public void testScrapeWithinInterval() {
		replayAll();
		Torrent torrent = createTorrent();

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
