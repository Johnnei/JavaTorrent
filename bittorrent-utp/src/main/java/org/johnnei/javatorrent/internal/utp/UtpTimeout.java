package org.johnnei.javatorrent.internal.utp;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.max;

/**
 * A class which is capable of calculating and updating the timeout value based on the arrived packets.
 */
public class UtpTimeout {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpTimeout.class);

	private Duration timeout;

	private int rtt;

	private int rttVariance;

	public UtpTimeout() {
		timeout = Duration.of(1000, ChronoUnit.MILLIS);
	}

	public void update(int receiveTime, UtpPacket ackedPacket) {
		// Calculate RTT and RTT Variance, and update the timeout value accordingly.
		int packetRtt = receiveTime - ackedPacket.getSentTime();
		int delta = rtt - packetRtt;
		rttVariance += (Math.abs(delta) - rttVariance) / 4;
		rtt += (packetRtt - rtt) / 8;

		Duration oldTimeout = timeout;
		// Divide by 1000 to make the RTT measurements into millis.
		timeout = Duration.of(max((rtt + rttVariance * 4) / 1000, 500), ChronoUnit.MILLIS);
		LOGGER.trace("Timeout changed from {}ms to {}ms", oldTimeout.toMillis(), timeout.toMillis());
	}

	public Duration getDuration() {
		return timeout;
	}

}
