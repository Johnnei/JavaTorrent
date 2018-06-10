package org.johnnei.javatorrent.internal.utp;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

/**
 * This handler is responsible to reduce packet overhead on reliable/fast sockets by changing the amount of payload in DATA packets.
 */
public class PacketSizeHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(PacketSizeHandler.class);

	private static final int CHANGE_CONTROL = 150;

	private final SocketWindowHandler windowHandler;

	private final Map<Short, UtpPacket> packetsInFlight;

	private int packetSize;

	private int targetPacketCount;


	public PacketSizeHandler(SocketWindowHandler windowHandler) {
		this.windowHandler = windowHandler;
		packetsInFlight = new HashMap<>();
		packetSize = 150;
		targetPacketCount = 10;
	}

	public void onSentPacket(UtpPacket packet) {
		packetsInFlight.put(packet.getHeader().getSequenceNumber(), packet);
	}

	public void onReceivedPacket(UtpPacket packet) {
		if (packetsInFlight.remove(packet.getHeader().getAcknowledgeNumber()) == null) {
			return;
		}

		targetPacketCount = Math.max(10, targetPacketCount - 1);
		setPacketSize(windowHandler.getMaxWindow() / targetPacketCount);
		LOGGER.trace("PacketSize update: [{}] target packets of [{}] bytes", targetPacketCount, packetSize);
	}

	public void onPacketLoss() {
		targetPacketCount *= 2;
	}

	private void setPacketSize(int packetSize) {
		this.packetSize = Math.max(150, packetSize);
		// Avoid MTU fragmentation.
		this.packetSize = Math.min(1000, this.packetSize);
	}

	public void onTimeout() {
		targetPacketCount = 10;
		packetSize = 150;
		LOGGER.trace("PacketSize timeout: {} target packets of {} bytes", targetPacketCount, packetSize);
	}

	public int getPacketSize() {
		return packetSize;
	}

}
