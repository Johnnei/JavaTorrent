package org.johnnei.javatorrent.internal.network;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

public class TransferRate {

	private final Clock clock;

	private LocalDateTime lastPoll;

	private AtomicInteger transferredBytes;

	private int rate;

	public TransferRate(Clock clock) {
		this.clock = clock;
		transferredBytes = new AtomicInteger(0);
		lastPoll = LocalDateTime.now(clock);
	}

	public void addTransferredBytes(int count) {
		transferredBytes.addAndGet(count);
	}

	public void pollRate() {
		int bytes = transferredBytes.getAndSet(0);
		LocalDateTime now = LocalDateTime.now(clock);

		Duration pollDuration = Duration.between(lastPoll, now);
		rate = (int) ((bytes * 1000L) / Math.max(1, pollDuration.toMillis()));
	}

	public int getRate() {
		return rate;
	}
}
