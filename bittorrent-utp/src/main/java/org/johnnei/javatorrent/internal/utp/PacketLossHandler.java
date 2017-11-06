package org.johnnei.javatorrent.internal.utp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

/**
 * Keeps tracks of sent packets and ensures that they will be resend when they are considered lost by the protocol.
 */
public class PacketLossHandler {

	private final UtpSocket socket;

	private final Map<Short, UtpPacket> packetsInFlight;

	private final Collection<UtpPacket> pendingResend;

	private int lastAck;

	private int duplicateCount;

	public PacketLossHandler(UtpSocket socket) {
		this.socket = socket;
		this.packetsInFlight = new HashMap<>();
		pendingResend = new ArrayList<>();
	}

	public void onReceivedPacket(UtpPacket packet) {
		// Purge the second to last packet as we no longer need it to track packet loss of n + 1.
		packetsInFlight.remove((short) (packet.getHeader().getAcknowledgeNumber() - 1));

		if (Short.toUnsignedInt(packet.getHeader().getAcknowledgeNumber()) > lastAck) {
			lastAck = Short.toUnsignedInt(packet.getHeader().getAcknowledgeNumber());
			duplicateCount = 0;
		} else if (packet.getPayload().getType() == PacketType.STATE) {
			duplicateCount++;
		}

		final short nextPacketSeqNr = (short) (packet.getHeader().getAcknowledgeNumber() + 1);
		synchronized (this) {
			if (duplicateCount >= 3 && canResendPacket(nextPacketSeqNr)) {
				UtpPacket nextPacket = packetsInFlight.get(nextPacketSeqNr);
				socket.resend(nextPacket);
				pendingResend.add(nextPacket);
			}
		}
	}

	public void onSentPacket(UtpPacket packet) {
		synchronized (this) {
			packetsInFlight.computeIfAbsent(packet.getHeader().getSequenceNumber(), s -> packet).incrementSentCount();
			pendingResend.removeIf(p -> p.getHeader().getSequenceNumber() == packet.getHeader().getSequenceNumber());
		}
	}

	private boolean canResendPacket(short sequenceNumber) {
		return packetsInFlight.containsKey(sequenceNumber) &&
			pendingResend.stream().noneMatch(packet -> packet.getHeader().getSequenceNumber() == sequenceNumber);
	}
}
