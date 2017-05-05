package org.johnnei.javatorrent.internal.utp;

import java.util.HashMap;
import java.util.Map;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

/**
 * Manages the packets which are in flight and responds to acknowledgements.
 */
public class PacketAckHandler {

	private final UtpSocket socket;

	/**
	 * The last confirmed packet to have been received.
	 */
	private short acknowledgeNumber;

	private Map<Short, Acknowledgement> acknowledgements;

	public PacketAckHandler(UtpSocket socket, short acknowledgeNumber) {
		this.socket = socket;
		this.acknowledgeNumber = acknowledgeNumber;
		this.acknowledgements = new HashMap<>();
	}

	/**
	 * Processes the acknowledgement in the received packet and potentially marks a packet to be resent.
	 * @param packet The received packet.
	 */
	public void onReceivedPacket(UtpPacket packet) {
		short sequenceNumber = packet.getHeader().getSequenceNumber();
		Acknowledgement acknowledgement = acknowledgements.computeIfAbsent(sequenceNumber, Acknowledgement::new);
		acknowledgement.packetSeen();

		while (acknowledgement != null && isNextPacketToAcknowledge(acknowledgement)) {
			socket.acknowledgePacket(acknowledgement);
			acknowledgeNumber = sequenceNumber;

			sequenceNumber++;
			acknowledgement = acknowledgements.get(sequenceNumber);
		}
	}

	private boolean isNextPacketToAcknowledge(Acknowledgement acknowledgement) {
		short nextPacket = (short) (acknowledgeNumber + 1);
		return acknowledgement.getSequenceNumber() == nextPacket;
	}
}
