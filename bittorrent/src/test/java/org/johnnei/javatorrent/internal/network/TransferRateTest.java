package org.johnnei.javatorrent.internal.network;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.test.TestClock;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class TransferRateTest {

	@Test
	public void testGetRate() {
		Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		TestClock clock = new TestClock(fixedClock);

		TransferRate cut = new TransferRate(clock);
		cut.addTransferredBytes(5);

		clock.setClock(Clock.offset(fixedClock, Duration.ofSeconds(1)));
		cut.pollRate();

		assertThat("Rate should be exactly 5 bytes/s", cut.getRate(), equalTo(5));

	}

}
