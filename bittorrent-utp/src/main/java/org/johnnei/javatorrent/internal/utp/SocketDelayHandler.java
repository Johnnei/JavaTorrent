package org.johnnei.javatorrent.internal.utp;

import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

/**
 * Handles the measuring of the 'delay' on received packets.
 * These delay values have in absolute terms no value as both clocks are very unlike to be in-sync.
 * Thus should be used as delta's of each other to indicate trends.
 */
public class SocketDelayHandler {

	private final PrecisionTimer timer;

	private int measuredDelay;

	public SocketDelayHandler(PrecisionTimer timer) {
		this.timer = timer;
		// When no delays are measured the value 0 must be reported.
		this.measuredDelay = 0;
	}

	public void onReceivedPacket(UtpPacket packet) {
		this.measuredDelay = timer.getCurrentMicros() - packet.getHeader().getTimestamp();
	}

	public int getMeasuredDelay() {
		return measuredDelay;
	}
}
