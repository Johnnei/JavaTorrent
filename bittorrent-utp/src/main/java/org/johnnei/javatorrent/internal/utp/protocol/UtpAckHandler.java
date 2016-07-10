package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utils.RecentLinkedList;
import org.johnnei.javatorrent.internal.utp.protocol.payload.StatePayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Short.toUnsignedInt;

/**
 * A class which manages the tracking of which packets are still considered in flight.
 */
public class UtpAckHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpAckHandler.class);

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	/**
	 * Packets which have been received out of order.
	 */
	private final Set<Short> futurePackets;

	/**
	 * The socket for which we are tracking ACKs.
	 */
	private final UtpSocketImpl socket;

	/**
	 * Packets which we've sent but aren't ACK'ed yet.
	 */
	private Collection<UtpPacket> packetsInFlight;

	/**
	 * If we've received the first packet.
	 * This is used to initialise the value for {@link #acknowledgeNumber}
	 */
	private AtomicBoolean firstPacket;

	/**
	 * The latest packet which we've received in order from the other end.
	 */
	private short acknowledgeNumber;

	private RecentLinkedList<Acknowledgement> acknowledgements;

	/**
	 * Creates a new Utp Acknowledgement handler.
	 *
	 * @param socket The socket for which this handler is handling acknowledgements.
	 */
	public UtpAckHandler(UtpSocketImpl socket) {
		this.socket = socket;
		packetsInFlight = new LinkedList<>();
		acknowledgements = new RecentLinkedList<>(10);
		futurePackets = new HashSet<>();
		firstPacket = new AtomicBoolean(true);
	}

	/**
	 * Registers the we've sent a new packet.
	 *
	 * @param packet The packet we've sent.
	 */
	public void registerPacket(UtpPacket packet) {
		if (packet.getTimesSent() != 0) {
			// Only register the packet if this is the first time it will be send.
			return;
		}

		if (packet.getType() == UtpProtocol.ST_STATE) {
			// There won't be ACKs for state packets, so don't add it.
			return;
		}

		lock.writeLock().lock();
		try {
			Optional<UtpPacket> duplicatePacket = packetsInFlight.stream()
					.filter(p -> packet.getSequenceNumber() == p.getSequenceNumber())
					.findAny();

			if (duplicatePacket.isPresent()) {
				return;
			}

			packetsInFlight.add(packet);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * Event to be called on every packet we receive from the other end.
	 *
	 * @param receivedPacket The packet we received.
	 * @return The packet which has been acked.
	 */
	public Optional<UtpPacket> onReceivedPacket(UtpPacket receivedPacket) throws IOException {
		if (firstPacket.compareAndSet(true, false)) {
			LOGGER.trace("Initialised base acknowledgeNumber to be {}", Short.toUnsignedInt(receivedPacket.getSequenceNumber()));
			// If the atomic set passed then we are receiving the first packet, assign the ack number here as a base point.
			acknowledgeNumber = receivedPacket.getSequenceNumber();
			socket.bindIoStreams(acknowledgeNumber);
		} else {
			updateAcknowledgeNumber(receivedPacket);
		}

		Optional<UtpPacket> ackedPacket = Optional.empty();
		lock.readLock().lock();
		try {
			// Find the packet which was ack'ed.
			ackedPacket = packetsInFlight.stream()
					.filter(packet -> packet.getSequenceNumber() == receivedPacket.getAcknowledgeNumber())
					.findAny();
		} finally {
			lock.readLock().unlock();
		}

		lock.writeLock().lock();
		try {
			// Remove all packets which are in flight which have a sequence number _before_ the acked packet.
			// We'll 'lose out' on the timestamp measurements if we'd still receive the separate ACK packets but this will clear out the window correctly.
			packetsInFlight.removeIf(p -> p.getSequenceNumber() == receivedPacket.getAcknowledgeNumber());
		} finally {
			lock.writeLock().unlock();
		}

		handleResend(receivedPacket);

		return ackedPacket;
	}

	private void handleResend(UtpPacket packet) throws IOException {
		Acknowledgement acknowledgement = acknowledgements.putIfAbsent(new Acknowledgement(packet.getAcknowledgeNumber()));
		acknowledgement.incrementCount();

		// Every 3 times we receive a duplicate we'll resend the packet.
		// In case of high packet loss the resend might drop so we allow multiple resends.
		if (acknowledgement.getCount() % 3 != 0) {
			return;
		}

		acknowledgement.resetCount();
		short nextSequenceNumber = (short) (packet.getAcknowledgeNumber() + 1);
		lock.readLock().lock();
		Optional<UtpPacket> lostPacketOptional = Optional.empty();
		try {
			lostPacketOptional = packetsInFlight.stream().filter(p -> {
				short s = p.getSequenceNumber();
				return s == nextSequenceNumber;
			}).findAny();
		} finally {
			lock.readLock().unlock();
		}

		if (!lostPacketOptional.isPresent()) {
			LOGGER.debug("Packet seq={} appears to be lost, but we've seen an ACK for it so it can't be resend.", Short.toUnsignedInt(nextSequenceNumber));
			return;
		}

		UtpPacket lostPacket = lostPacketOptional.get();
		LOGGER.trace("Resending lost packet {}", lostPacket);
		socket.sendUnbounded(lostPacket);
	}

	private void updateAcknowledgeNumber(UtpPacket packet) throws IOException {
		if (packet.getType() == UtpProtocol.ST_STATE) {
			// Don't process ST_STATE packets.
			return;
		}

		synchronized (futurePackets) {
			futurePackets.add(packet.getSequenceNumber());

			// TODO Always send out a state packet.

			while (true) {
				short nextPacket = (short) (acknowledgeNumber + 1);
				if (futurePackets.remove(nextPacket)) {
					acknowledgeNumber = nextPacket;

					UtpPacket statePacket = new UtpPacket(socket, new StatePayload());
					LOGGER.trace("Sending ACK message for {}, caused by {}.", Short.toUnsignedInt(acknowledgeNumber), packet);
					socket.sendUnbounded(statePacket);
					LOGGER.trace("Sent ACK {} for {}.", statePacket, Short.toUnsignedInt(acknowledgeNumber));
				} else {
					break;
				}
			}
		}
	}

	/**
	 * Event to be called when {@link UtpProtocol#ST_RESET} is received.
	 */
	public void onReset() {
		lock.writeLock().lock();
		try {
			packetsInFlight.clear();
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * @return The latest packet which we've received in order from the other end.
	 */
	public short getAcknowledgeNumber() {
		return acknowledgeNumber;
	}

	/**
	 * @return The sum of all the packet sizes which have not yet been ACK'ed.
	 */
	public int countBytesInFlight() {
		lock.readLock().lock();
		try {
			return packetsInFlight.stream().mapToInt(UtpPacket::getPacketSize).sum();
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * @return <code>true</code> when there is at least one packet in flight.
	 */
	public boolean hasPacketsInFlight() {
		return !packetsInFlight.isEmpty();
	}

	/**
	 * @return <code>true</code> when the socket has engaged with the handshake and thus has a base {@link #acknowledgeNumber} set.
	 */
	public boolean isInitialised() {
		return !firstPacket.get();
	}

	@Override
	public String toString() {
		return String.format("UtpAckHandler[ack=%d, packetsInFlight=%d, bytes in flight=%d]", toUnsignedInt(acknowledgeNumber), packetsInFlight.size(), countBytesInFlight());
	}

	private final class Acknowledgement {

		private final int sequenceNumber;

		private int count;

		Acknowledgement(int sequenceNumber) {
			this.sequenceNumber = sequenceNumber;
			count = 0;
		}

		void incrementCount() {
			count++;
		}

		void resetCount() {
			count = 0;
		}

		int getCount() {
			return count;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}

			if (o == null) {
				return false;
			}

			if (!(o instanceof Acknowledgement)) {
				return false;
			}

			Acknowledgement that = (Acknowledgement) o;
			return sequenceNumber == that.sequenceNumber;
		}

		@Override
		public int hashCode() {
			return Objects.hash(sequenceNumber);
		}
	}

}
