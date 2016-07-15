package org.johnnei.javatorrent.internal.utp;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * A class which is capable of calculating and updating the timeout value based on the arrived packets.
 */
public class UtpTimeout {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpTimeout.class);

	/**
	 * The maximum timeout duration in milliseconds.
	 */
	private static final int MAX_TIMEOUT = 10_000;

	/**
	 * The minimum timeout duration in milliseconds.
	 */
	private static final int MIN_TIMEOUT = 500;

	private Duration timeout;

	private int rtt;

	private int rttVariance;

	/**
	 * Creates a new UTP Timeout handler with the initial timeout of 1 second.
	 */
	public UtpTimeout() {
		timeout = Duration.of(1000, ChronoUnit.MILLIS);
	}

	/**
	 * Calculates the new timeout based on the round trip time of a packet.
	 * @param receiveTime The time as close as possible to the time of receiving.
	 * @param packet The packet which has completed the round trip.
	 */
	public void update(int receiveTime, UtpPacket packet) {
		if (packet.getTimesSent() > 1) {
			// Don't include packets which have been reset.
			return;
		}

		// Calculate RTT and RTT Variance, and update the timeout value accordingly.
		int packetRtt = receiveTime - packet.getSentTime();
		int delta = rtt - packetRtt;
		rttVariance += (Math.abs(delta) - rttVariance) / 4;
		rtt += (packetRtt - rtt) / 8;

		Duration oldTimeout = timeout;
		// Divide by 1000 to make the RTT measurements into millis.
		timeout = Duration.of(min(MAX_TIMEOUT, max((rtt + rttVariance * 4) / 1000, MIN_TIMEOUT)), ChronoUnit.MILLIS);

		if (timeout.toMillis() != oldTimeout.toMillis()) {
			LOGGER.trace("Timeout changed from {}ms to {}ms", oldTimeout.toMillis(), timeout.toMillis());
		}
	}

	/**
	 * @return The timeout duration.
	 */
	public Duration getDuration() {
		return timeout;
	}

}
