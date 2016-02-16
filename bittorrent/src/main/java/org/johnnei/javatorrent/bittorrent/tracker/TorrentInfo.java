package org.johnnei.javatorrent.bittorrent.tracker;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.johnnei.javatorrent.torrent.Torrent;

public class TorrentInfo {

	public static final Duration DEFAULT_ANNOUNCE_INTERVAL = Duration.of(30, ChronoUnit.SECONDS);

	/**
	 * The clock instance to obtain the time
	 */
	private Clock clock;

	/**
	 * The torrent for which this info is being stored
	 */
	private Torrent torrent;

	/**
	 * The timestamp of the last announce
	 */
	private LocalDateTime lastAnnounceTime;

	/**
	 * The amount of seeders as reported by the tracker
	 */
	private int seeders;

	/**
	 * The amount of leechers as reported by the tracker
	 */
	private int leechers;

	/**
	 * The amount of times this torrent has been download as reported by the tracker
	 */
	private int downloaded;

	/**
	 * The current event
	 */
	private TrackerEvent event;

	public TorrentInfo(Torrent torrent, Clock clock) {
		this.torrent = torrent;
		this.clock = clock;
		this.event = TrackerEvent.EVENT_STARTED;
		lastAnnounceTime = LocalDateTime.now(clock).minus(DEFAULT_ANNOUNCE_INTERVAL);
	}

	public void updateAnnounceTime() {
		lastAnnounceTime = LocalDateTime.now(clock);
	}

	public void setEvent(TrackerEvent event) {
		this.event = event;
	}

	public void setInfo(int seeders, int leechers) {
		this.seeders = seeders;
		this.leechers = leechers;
	}

	public void setInfo(int seeders, int leechers, int downloadCount) {
		this.seeders = seeders;
		this.leechers = leechers;
		this.downloaded = downloadCount;
	}

	public TrackerEvent getEvent() {
		return event;
	}

	/**
	 * The amount of seeders as reported by the tracker
	 *
	 * @return the amount of seeders
	 */
	public int getSeeders() {
		return seeders;
	}

	/**
	 * The amount of leechers as reported by the tracker
	 *
	 * @return the amount of leechers
	 */
	public int getLeechers() {
		return leechers;
	}

	/**
	 * The amount of times this torrent has been downloaded<br/>
	 * If the tracker returns 0 it will return N/A as the tracker apparently doesn't support it
	 *
	 * @return the count of times downloaded or N/A if not reported
	 */
	public String getDownloadCount() {
		return (downloaded == 0) ? "N/A" : Integer.toString(downloaded);
	}

	/**
	 * The time since the last announce
	 *
	 * @return The duration since last announce
	 */
	public Duration getTimeSinceLastAnnouce() {
		return Duration.between(lastAnnounceTime, LocalDateTime.now(clock));
	}

	/**
	 * Gets the associated torrent
	 *
	 * @return The torrent with this info
	 */
	public Torrent getTorrent() {
		return torrent;
	}

}
