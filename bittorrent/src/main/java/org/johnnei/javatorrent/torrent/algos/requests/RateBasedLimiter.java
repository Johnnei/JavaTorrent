package org.johnnei.javatorrent.torrent.algos.requests;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.OptionalDouble;

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
			rateInfo.getAverage(clock).ifPresent(avg -> {
				int blocksPerSecond = (int) (Duration.ofSeconds(1).toMillis() / Math.max(Double.MIN_NORMAL, avg));
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
			readTimes.add(new RateEntry(clock.instant(), readTime));
		}

		OptionalDouble getAverage(Clock clock) {
			cleanReadTimes(clock);
			return readTimes.stream().mapToLong(e -> e.readTime.toMillis()).average();
		}

		private void cleanReadTimes(Clock clock) {
			Instant recentItems = clock.instant().minusSeconds(5);
			readTimes.removeIf(e -> e.timestamp.isBefore(recentItems));
		}

	}

	private static class RateEntry {

		private final Instant timestamp;

		private final Duration readTime;

		RateEntry(Instant instant, Duration readTime) {
			this.readTime = readTime;
			this.timestamp = instant;
		}

	}
}
