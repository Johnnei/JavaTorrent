package org.johnnei.javatorrent.internal.utp;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utils.MathUtils;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.toUnsignedLong;

/**
 * Calculates the Window of an uTP socket.
 */
public class UtpWindow {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpWindow.class);

	private static final long CONGESTION_CONTROL_TARGET = Duration.of(100, ChronoUnit.MILLIS).toNanos() / 1000;

	private static final int MAX_WINDOW_CHANGE_PER_PACKET = 100;

	private UtpSocketImpl socket;

	private SlidingTimedValue<Integer> slidingBaseDelay;

	private int maxWindow;

	public UtpWindow(UtpSocketImpl socket) {
		this.socket = socket;
		slidingBaseDelay = new SlidingTimedValue<>();
		maxWindow = 150;
	}

	public void update(UtpPacket packet) {
		slidingBaseDelay.addValue(packet.getTimestampDifferenceMicroseconds());
		long ourDelay = toUnsignedLong(packet.getTimestampDifferenceMicroseconds()) - toUnsignedLong(slidingBaseDelay.getMinimum());
		long offTarget = CONGESTION_CONTROL_TARGET - ourDelay;
		double delayFactor = MathUtils.clamp(-1, 1, offTarget / (double) CONGESTION_CONTROL_TARGET);
		double windowFactor = MathUtils.clamp(-1, 1, socket.getBytesInFlight() / (double) maxWindow);
		int scaledGain = (int) (MAX_WINDOW_CHANGE_PER_PACKET * delayFactor * windowFactor);

		maxWindow += scaledGain;
		LOGGER.trace("Base Delay: {}us, Packet Delay: {}us, Delay: {}us, Off Target: {}us, Delay Factor: {}, Window Factor: {}.",
				slidingBaseDelay.getMinimum(),
				packet.getTimestampDifferenceMicroseconds(),
				ourDelay,
				offTarget,
				delayFactor,
				windowFactor);
		LOGGER.trace("Updated window size based on ACK. Window changed by {}, Now: {}",
				scaledGain,
				maxWindow);
	}

	public int getSize() {
		return maxWindow;
	}

	public void onTimeout() {
		// TODO Make this neater, according to the spec this should SET to 150 as it will allow one more packet to be sent.
		// This implementation won't do that though. So maybe I think of maxWindow incorrectly?
		maxWindow += 150;
	}
}
