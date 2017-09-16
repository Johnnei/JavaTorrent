package org.johnnei.javatorrent.internal.utp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

/**
 * Keeps tracks of sent packets and ensures that they will be resend when they are considered lost by the protocol.
 */
public class PacketLossHandler {

	private final UtpSocket socket;

	private final Map<Short, PacketRegistration> packetsInFlight;

	private final Collection<UtpPacket> pendingResend;

	public PacketLossHandler(UtpSocket socket) {
		this.socket = socket;
		this.packetsInFlight = new HashMap<>();
		pendingResend = new ArrayList<>();
	}

	public void onReceivedPacket(UtpPacket packet) {
		// Purge the second to last packet as we no longer need it to track packet loss of n + 1.
		packetsInFlight.remove((short) (packet.getHeader().getAcknowledgeNumber() - 1));

		PacketRegistration registration = packetsInFlight.get(packet.getHeader().getAcknowledgeNumber());

		if (registration == null) {
			// When a connection gets accepted the first packet received contains a non-existing ack_nr.
			return;
		}

		registration.acknowledgement.packetSeen();

		final short nextPacketSeqNr = (short) (packet.getHeader().getAcknowledgeNumber() + 1);
		synchronized (this) {
			if (registration.acknowledgement.getTimesSeen() >= 3 && canResendPacket(nextPacketSeqNr)) {
				UtpPacket nextPacket = packetsInFlight.get(nextPacketSeqNr).packet;
				socket.resend(nextPacket);
				pendingResend.add(nextPacket);
			}
		}
	}

	public void onSentPacket(UtpPacket packet) {
		synchronized (this) {
			packetsInFlight.computeIfAbsent(packet.getHeader().getSequenceNumber(), s -> new PacketRegistration(packet)).packet.incrementSentCount();
			pendingResend.removeIf(p -> p.getHeader().getSequenceNumber() == packet.getHeader().getSequenceNumber());
		}
	}

	private boolean canResendPacket(short sequenceNumber) {
		return packetsInFlight.containsKey(sequenceNumber) &&
			pendingResend.stream().noneMatch(packet -> packet.getHeader().getSequenceNumber() == sequenceNumber);
	}

	private static final class PacketRegistration {

		final UtpPacket packet;

		final Acknowledgement acknowledgement;

		PacketRegistration(UtpPacket packet) {
			this.packet = packet;
			acknowledgement = new Acknowledgement(packet.getHeader().getSequenceNumber());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			PacketRegistration that = (PacketRegistration) o;
			return Objects.equals(acknowledgement, that.acknowledgement);
		}

		@Override
		public int hashCode() {
			return Objects.hash(acknowledgement);
		}

		@Override
		public String toString() {
			return String.format("PacketRegistration[packet=%s]", packet);
		}
	}
}
