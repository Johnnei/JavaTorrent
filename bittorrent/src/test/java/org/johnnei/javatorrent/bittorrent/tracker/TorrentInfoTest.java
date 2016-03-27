package org.johnnei.javatorrent.bittorrent.tracker;

import java.time.Clock;
import java.time.Duration;

import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.torrent.Torrent;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

		assertEquals("Seeder count should have been 15", 15, cut.getSeeders());
		assertEquals("Leechers count should have been 42", 42, cut.getLeechers());
		assertEquals("Duration since last announce should have been zero", Duration.ZERO, cut.getTimeSinceLastAnnounce());
	}

	@Test
	public void testSetInfoScrape() {
		Torrent torrent = DummyEntity.createUniqueTorrent();

		TorrentInfo cut = new TorrentInfo(torrent, Clock.systemDefaultZone());

		assertEquals("Download count should have been 0 causing N/A", "N/A", cut.getDownloadCount());

		cut.setInfo(15, 42, 10);

		assertEquals("Seeder count should have been 15", 15, cut.getSeeders());
		assertEquals("Leechers count should have been 42", 42, cut.getLeechers());
		assertEquals("Download count should have been 10", "10", cut.getDownloadCount());
	}

	@Test
	public void testGettersAndSetters() {
		Torrent torrent = DummyEntity.createUniqueTorrent();

		TorrentInfo cut = new TorrentInfo(torrent, Clock.systemDefaultZone());

		assertEquals("Torrent should be the same as the given one in the constructor", torrent, cut.getTorrent());
		assertEquals("Initial tracker event should be STARTED", TrackerEvent.EVENT_STARTED, cut.getEvent());

		cut.setEvent(TrackerEvent.EVENT_COMPLETED);

		assertEquals("Event should have changed by set.", TrackerEvent.EVENT_COMPLETED, cut.getEvent());
	}

}