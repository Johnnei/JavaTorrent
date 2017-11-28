package org.johnnei.javatorrent.tracker;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
import org.johnnei.javatorrent.internal.tracker.udp.IUdpTrackerPayload;
import org.johnnei.javatorrent.internal.tracker.udp.UdpTrackerSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.torrent.Torrent;

import static org.johnnei.javatorrent.test.DummyEntity.createUniqueTorrent;
import static org.johnnei.javatorrent.test.TestUtils.assertEqualsMethod;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UdpTrackerTest {

	private UdpTracker cut;

	private TorrentClient torrentClientMock;

	private UdpTrackerSocket udpTrackerSocketMock;

	private TestClock clock;

	private Clock fixedClock;

	@BeforeEach
	public void setUp() throws Exception {
		udpTrackerSocketMock = mock(UdpTrackerSocket.class);
		fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		clock = new TestClock(fixedClock);
		torrentClientMock = mock(TorrentClient.class);
		cut = new UdpTracker.Builder()
				.setTorrentClient(torrentClientMock)
				.setSocket(udpTrackerSocketMock)
				.setUrl("udp://localhost:80")
				.setClock(clock)
				.build();
	}

	@Test
	public void testUdpTrackerConstructorWithIncorrectProtocol() throws Exception {
		assertThrows(TrackerException.class, () -> new UdpTracker.Builder()
			.setUrl("http://localhost:80")
			.build());
	}

	@Test
	public void testUdpTrackerConstructorWithIncorrectDomain() throws Exception {
		UdpTracker tracker = new UdpTracker.Builder()
			.setUrl("udp://127.0.0.0.1:80")
			.build();

		assertEquals(0x41727101980L, tracker.getConnection().getId(), "Incorrect connection id");
		assertEquals("Unknown", tracker.getName(), "Incorrect name");
		assertEquals("Invalid tracker", tracker.getStatus(), "Incorrect state");
	}

	@Test
	public void testEqualsAndHashCode() throws Exception {
		UdpTracker tracker = new UdpTracker.Builder()
				.setUrl("udp://127.0.0.1:80")
				.build();
		UdpTracker trackerTwo = new UdpTracker.Builder()
				.setUrl("udp://127.0.0.1:80")
				.build();
		UdpTracker trackerThree = new UdpTracker.Builder()
				.setUrl("udp://127.0.0.1:8080")
				.build();

		assertEqualsMethod(tracker);
		assertTrue(tracker.equals(trackerTwo), "Trackers with same URL didn't match");
		assertEquals(tracker.hashCode(), trackerTwo.hashCode(), "Trackers with same URL procuded different hash");
		assertFalse(tracker.equals(trackerThree), "Trackers with different URL matched");
	}

	@Test
	public void testAddAndHasTorrent() {
		Torrent torrent = createUniqueTorrent();

		assertFalse(cut.hasTorrent(torrent), "Has torrent before adding");
		cut.addTorrent(torrent);
		assertTrue(cut.hasTorrent(torrent), "Doesn't have torrent after adding");

		TorrentInfo info = cut.getInfo(torrent).get();
		info.setInfo(5, 6);

		cut.addTorrent(torrent);

		info = cut.getInfo(torrent).get();
		assertEquals(5, info.getSeeders(), "Incorrect seeder count (Torrent info got overwritten)");
		assertEquals(6, info.getLeechers(), "Incorrect leechers count (Torrent info got overwritten)");
	}

	@Test
	public void testAnnounce() throws Exception {
		ArgumentCaptor<IUdpTrackerPayload> payloadCapture = ArgumentCaptor.forClass(IUdpTrackerPayload.class);

		when(torrentClientMock.getDownloadPort()).thenReturn(27960);
		when(torrentClientMock.getPeerId()).thenReturn(DummyEntity.createPeerId());

		Torrent torrent = createUniqueTorrent();

		cut.addTorrent(torrent);
		cut.announce(torrent);

		verify(udpTrackerSocketMock).submitRequest(same(cut), payloadCapture.capture());
		assertEquals(TrackerAction.ANNOUNCE, payloadCapture.getValue().getAction(), "Incorrect Tracker request type");
	}

	@Test
	public void testAnnounceWithinInterval() throws Exception {
		Torrent torrent = createUniqueTorrent();

		cut.addTorrent(torrent);
		// Move clock back to simulate that we're still in the interval period
		clock.setClock(Clock.offset(fixedClock, Duration.ofSeconds(-10)));
		cut.announce(torrent);
	}

	@Test
	public void testGetInfo() {
		Torrent torrentOne = createUniqueTorrent();
		Torrent torrentTwo = createUniqueTorrent();
		Torrent torrentThree = createUniqueTorrent();

		cut.addTorrent(torrentOne);
		cut.addTorrent(torrentTwo);

		TorrentInfo torrentInfoOne = cut.getInfo(torrentOne).orElseThrow(() -> new AssertionError("Null on existing torrent"));
		assertEquals(torrentOne, torrentInfoOne.getTorrent(), "Torrent info did not match torrent");

		TorrentInfo torrentInfoTwo = cut.getInfo(torrentTwo).orElseThrow(() -> new AssertionError("Null on existing torrent"));
		assertEquals(torrentTwo, torrentInfoTwo.getTorrent(), "Torrent info did not match torrent");

		assertEquals(Optional.empty(), cut.getInfo(torrentThree), "Torrent info returned for not registered torrent");
	}

	@Test
	public void testScrape() throws Exception {
		ArgumentCaptor<IUdpTrackerPayload> payloadCapture = ArgumentCaptor.forClass(IUdpTrackerPayload.class);

		Torrent torrent = createUniqueTorrent();

		cut.addTorrent(torrent);
		cut.scrape();

		verify(udpTrackerSocketMock).submitRequest(same(cut), payloadCapture.capture());

		assertEquals(TrackerAction.SCRAPE, payloadCapture.getValue().getAction(), "Incorrect Tracker request type");
	}

	@Test
	public void testScrapeWithinInterval() {
		Torrent torrent = createUniqueTorrent();

		// Move clock back to simulate that we're still in the interval period
		clock.setClock(Clock.offset(fixedClock, Duration.ofSeconds(-10)));

		cut.addTorrent(torrent);
		cut.scrape();
	}


	@Disabled("Feature not yet supported")
	@Test
	public void testScrapeMultiTorrent() {
		fail("Not yet implemented");
	}

	/**
	 * Tests a scrape with 75 torrents which 1 above the protocol limit.
	 */
	@Disabled("Feature not yet supported")
	@Test
	public void testScrapeMultiTorrentLimit() {
		fail("Not yet implemented");
	}

}
