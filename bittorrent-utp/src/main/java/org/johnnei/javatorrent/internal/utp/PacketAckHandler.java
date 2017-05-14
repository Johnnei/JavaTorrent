package org.johnnei.javatorrent.internal.utp;

import java.util.HashMap;
import java.util.Map;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

/**
 * Manages the packets which are in flight and responds to acknowledgements.
 */
public class PacketAckHandler {

	private final UtpSocket socket;

	private boolean isInitialized;

	/**
	 * The last confirmed packet to have been received.
	 */
	private short acknowledgeNumber;

	private Map<Short, Acknowledgement> acknowledgements;

	/**
	 * Created an Ack Handler with an uninitialized initial packet. The first packet passed in {@link #onReceivedPacket(UtpPacket)} will be considered the first
	 * packet to ack.
	 * @param socket The socket which has to ack the received packets.
	 */
	public PacketAckHandler(UtpSocket socket) {
		this.socket = socket;
		this.acknowledgements = new HashMap<>();
		isInitialized = false;
	}

	/**
	 * Created an Ack Handler with an initialized initial packet.
	 * @param socket The socket which has to ack the received packets.
	 */
	public PacketAckHandler(UtpSocket socket, short acknowledgeNumber) {
		this.socket = socket;
		this.acknowledgements = new HashMap<>();
		this.acknowledgeNumber = acknowledgeNumber;
		isInitialized = true;
	}

	/**
	 * Processes the acknowledgement in the received packet and potentially marks a packet to be resent.
	 * @param packet The received packet.
	 */
	public void onReceivedPacket(UtpPacket packet) {
		short sequenceNumber = packet.getHeader().getSequenceNumber();
		Acknowledgement acknowledgement = acknowledgements.computeIfAbsent(sequenceNumber, Acknowledgement::new);
		acknowledgement.packetSeen();

		if (!isInitialized) {
			isInitialized = true;
			/*
			 * The first packet sent after receiving the ST_STATE on ST_SYN must be ST_DATA.
			 * So don't explicitly request this packet to be ACK'ed but rely on it being included as the ack field on the first ST_DATA being sent out.
			 */
			acknowledgeNumber = sequenceNumber;
		}

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
