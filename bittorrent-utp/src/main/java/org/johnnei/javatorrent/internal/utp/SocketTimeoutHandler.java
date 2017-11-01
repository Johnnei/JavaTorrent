package org.johnnei.javatorrent.internal.utp;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

public class SocketTimeoutHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(SocketTimeoutHandler.class);

	/**
	 * The minimum timeout in milliseconds as defined by BEP-29.
	 */
	private static final Duration MINIMUM_TIMEOUT = Duration.of(500, ChronoUnit.MILLIS);

	private final PrecisionTimer timer;

	private Duration timeout;

	private int lastActivity;

	private int roundTripTime;

	private int roundTripTimeVariance;

	public SocketTimeoutHandler(PrecisionTimer timer) {
		this.timer = timer;
		lastActivity = timer.getCurrentMicros();
		timeout = Duration.of(1000, ChronoUnit.MILLIS);
	}

	public void onReceivedPacket(UtpPacket packet) {
		int receiveTime = timer.getCurrentMicros();
		if (!packet.isSendOnce()) {
			LOGGER.trace("Ignoring re-sent packet [{}] for timeout adjustment.", Short.toUnsignedInt(packet.getHeader().getSequenceNumber()));
			return;
		}

		int packetRoundTripTime = receiveTime - packet.getHeader().getTimestamp();
		int delta = roundTripTime - packetRoundTripTime;
		int varianceAdjustment = (Math.abs(delta) - roundTripTimeVariance) / 4;
		int roundTripTimeAdjustment = (packetRoundTripTime - roundTripTime) / 8;

		roundTripTimeVariance += varianceAdjustment;
		roundTripTime += roundTripTimeAdjustment;

		Duration newTimeout = Duration.of(roundTripTime + roundTripTimeVariance * 4L, ChronoUnit.MICROS);
		timeout = Duration.of(Math.max(newTimeout.toMillis(), MINIMUM_TIMEOUT.toMillis()), ChronoUnit.MILLIS);
		LOGGER.trace(
			"Packet [{}] caused timeout to become [{}]. rtt += [{}], rtt_var += [{}]",
			Short.toUnsignedInt(packet.getHeader().getSequenceNumber()),
			timeout,
			roundTripTimeAdjustment,
			varianceAdjustment
		);
	}

	public void onSentPacket() {
		lastActivity = timer.getCurrentMicros();
	}

	public void onTimeout() {
		timeout = Duration.of(timeout.toMillis() * 2, ChronoUnit.MILLIS);
	}

	public boolean isTimeoutExpired() {
		return Duration.of((long) timer.getCurrentMicros() - lastActivity, ChronoUnit.MICROS).compareTo(timeout) > 0;
	}

	int getRoundTripTime() {
		return roundTripTime;
	}

	int getRoundTripTimeVariance() {
		return roundTripTimeVariance;
	}

	int getTimeout() {
		return (int) timeout.toMillis();
	}
}
