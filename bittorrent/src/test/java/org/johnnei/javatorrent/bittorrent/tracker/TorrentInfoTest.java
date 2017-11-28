package org.johnnei.javatorrent.bittorrent.tracker;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.torrent.Torrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests {@link TorrentInfo}
 */
public class TorrentInfoTest {

	@Test
	public void testSetInfoAnnounce() {
		Torrent torrent = DummyEntity.createUniqueTorrent();

		Clock baseClock = Clock.fixed(Clock.systemDefaultZone().instant(), Clock.systemDefaultZone().getZone());
		Clock offsetClock = Clock.offset(baseClock, Duration.ofSeconds(3));

		TestClock testClock = new TestClock(baseClock);

		TorrentInfo cut = new TorrentInfo(torrent, testClock);

		testClock.setClock(offsetClock);

		cut.setInfo(15, 42);

		assertEquals(15, cut.getSeeders(), "Seeder count should have been 15");
		assertEquals(42, cut.getLeechers(), "Leechers count should have been 42");
		assertEquals(Duration.ZERO, cut.getTimeSinceLastAnnounce(), "Duration since last announce should have been zero");
	}

	@Test
	public void testSetInfoScrape() {
		Torrent torrent = DummyEntity.createUniqueTorrent();

		TorrentInfo cut = new TorrentInfo(torrent, Clock.systemDefaultZone());

		assertEquals("N/A", cut.getDownloadCount(), "Download count should have been 0 causing N/A");

		cut.setInfo(15, 42, 10);

		assertEquals(15, cut.getSeeders(), "Seeder count should have been 15");
		assertEquals(42, cut.getLeechers(), "Leechers count should have been 42");
		assertEquals("10", cut.getDownloadCount(), "Download count should have been 10");
	}

	@Test
	public void testGettersAndSetters() {
		Torrent torrent = DummyEntity.createUniqueTorrent();

		TorrentInfo cut = new TorrentInfo(torrent, Clock.systemDefaultZone());

		assertEquals(torrent, cut.getTorrent(), "Torrent should be the same as the given one in the constructor");
		assertEquals(TrackerEvent.EVENT_STARTED, cut.getEvent(), "Initial tracker event should be STARTED");

		cut.setEvent(TrackerEvent.EVENT_COMPLETED);

		assertEquals(TrackerEvent.EVENT_COMPLETED, cut.getEvent(), "Event should have changed by set.");
	}

}
