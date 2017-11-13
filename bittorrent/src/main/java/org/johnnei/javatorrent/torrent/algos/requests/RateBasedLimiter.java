package org.johnnei.javatorrent.torrent.algos.requests;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.torrent.peer.Peer;

/**
 * Created by johnn on 01/10/2016.
 */
public class RateBasedLimiter implements IRequestLimiter {

	private final Clock clock = Clock.systemDefaultZone();

	private static final int MAX_DECREASE = -2;

	private static final int MAX_INCREASE = 25;

	@Override
	public void onReceivedBlock(Peer peer, MessageBlock messageBlock) {
		messageBlock.getReadDuration().ifPresent(duration -> {
			RateInfo rateInfo = getRateInfo(peer);
			rateInfo.addEntry(clock, duration);
			rateInfo.getAvarage(clock).ifPresent(avg -> {
				int blocksPerSecond = (int) (Duration.ofSeconds(1).toNanos() / Math.max(1, avg.toNanos()));
				// Request blocks for the next three seconds.
				int diff = (3 * blocksPerSecond) - peer.getRequestLimit();
				if (diff < 0) {
					diff = Math.max(diff, MAX_DECREASE);
				} else {
					diff = Math.min(diff, MAX_INCREASE);
				}
				peer.setRequestLimit(Math.max(1, peer.getRequestLimit() + diff));
			});
		});
	}

	private RateInfo getRateInfo(Peer peer) {
		return peer.getModuleInfo(RateInfo.class).orElseGet(() -> {
			RateInfo rateInfo = new RateInfo();
			peer.addModuleInfo(rateInfo);
			return rateInfo;
		});
	}

	private static class RateInfo {

		private final List<RateEntry> readTimes;

		RateInfo() {
			readTimes = new LinkedList<>();
		}

		void addEntry(Clock clock, Duration readTime) {
			readTimes.add(new RateEntry(clock, readTime));
		}

		Optional<Duration> getAvarage(Clock clock) {
			cleanReadTimes(clock);
			return readTimes.stream().map(e -> e.readTime).reduce(Duration::plus).map(duration -> duration.dividedBy(readTimes.size()));
		}

		private void cleanReadTimes(Clock clock) {
			Instant recentItems = clock.instant().minusSeconds(5);
			readTimes.removeIf(e -> e.timestamp.isBefore(recentItems));
		}

	}

	private static class RateEntry {

		private final Instant timestamp;

		private final Duration readTime;

		RateEntry(Clock clock, Duration readTime) {
			this.readTime = readTime;
			timestamp = clock.instant();
		}

	}
}
