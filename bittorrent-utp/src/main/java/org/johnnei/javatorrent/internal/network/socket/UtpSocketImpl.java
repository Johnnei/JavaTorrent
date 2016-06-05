package org.johnnei.javatorrent.internal.network.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utils.Sync;
import org.johnnei.javatorrent.internal.utp.UtpTimeout;
import org.johnnei.javatorrent.internal.utp.UtpWindow;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpInputStream;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;
import org.johnnei.javatorrent.internal.utp.protocol.UtpOutputStream;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.payload.FinPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.IPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.ResetPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.StatePayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.SynPayload;
import org.johnnei.javatorrent.network.OutStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Math.max;

/**
 * Internal implementation of the {@link org.johnnei.javatorrent.network.socket.UtpSocket}
 */
public class UtpSocketImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpSocketImpl.class);

	private static final int PACKET_OVERHEAD = 20;
	private static final int UNKNOWN_TIMESTAMP_DIFFERENCE = 0;

	private final Lock notifyLock = new ReentrantLock();

	private final ReadWriteLock packetsInFlightLock = new ReentrantReadWriteLock();

	/**
	 * A condition which gets triggered when a packet is acknowledged and thus making room in the write queue
	 */
	private final Condition onPacketAcknowledged = notifyLock.newCondition();

	private final UtpMultiplexer utpMultiplexer;

	private Clock clock = Clock.systemDefaultZone();

	private PrecisionTimer timer = new PrecisionTimer();

	private SocketAddress socketAddress;

	private ConnectionState connectionState;

	private Collection<UtpPacket> packetsInFlight;

	private Instant lastInteraction;

	private UtpTimeout timeout;

	/**
	 * The sequence number which as marked as the last packet by {@link org.johnnei.javatorrent.internal.utp.protocol.payload.FinPayload}
	 */
	private short endOfStreamSequenceNumber;

	private short acknowledgeNumber;

	private short sequenceNumberCounter;

	private short connectionIdReceive;

	private short connectionIdSend;

	private int packetSize;

	/**
	 * The window size advertised by the other end.
	 */
	private int clientWindowSize;

	private UtpWindow window;

	private UtpInputStream inputStream;

	private UtpOutputStream outputStream;

	private short sequenceNumber;

	private int measuredDelay;

	/**
	 * Creates a new socket which is considered the initiating endpoint
	 */
	public UtpSocketImpl(UtpMultiplexer utpMultiplexer) {
		initDefaults();
		this.utpMultiplexer = utpMultiplexer;
		connectionIdReceive = (short) new Random().nextInt();
		connectionIdSend = (short) (connectionIdReceive + 1);
		sequenceNumberCounter = 1;
	}

	/**
	 * Creates a new socket which is considered the accepting endpoint.
	 * @param connectionId The connection id which we will use to send packets with.
	 */
	public UtpSocketImpl(UtpMultiplexer utpMultiplexer, SocketAddress socketAddress, short connectionId) {
		initDefaults();
		this.socketAddress = socketAddress;
		this.utpMultiplexer = utpMultiplexer;
		connectionIdReceive = (short) (connectionId + 1);
		connectionIdSend = connectionId;
		sequenceNumberCounter = (short) new Random().nextInt();
	}

	private void initDefaults() {
		connectionState = ConnectionState.CONNECTING;
		packetsInFlight = new LinkedList<>();
		packetSize = 150;
		clientWindowSize = 150;
		timeout = new UtpTimeout();
		window = new UtpWindow(this);
		lastInteraction = clock.instant();
	}

	public void connect(InetSocketAddress endpoint) throws IOException {
		socketAddress = endpoint;
		connectionState = ConnectionState.CONNECTING;
		send(new SynPayload());

		// Wait for the acknowledgement of the connection.
		notifyLock.lock();
		try {
			onPacketAcknowledged.await(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			throw new IOException("Interruption while waiting for connection confirmation.");
		} finally {
			notifyLock.unlock();
		}

		if (connectionState != ConnectionState.CONNECTED) {
			throw new IOException("Endpoint did not respond to connection attempt");
		}
	}

	public void send(IPayload payload) throws IOException {
		UtpPacket packet = new UtpPacket(this, payload);

		// Wait for enough space to write the packet
		while (connectionState != ConnectionState.CLOSED && getAvailableWindowSize() < packet.getPacketSize()) {
			LOGGER.trace("Waiting to send packet of {} bytes. Window Status: {} / {} bytes", packet.getPacketSize(), getBytesInFlight(), getSendWindowSize());
			notifyLock.lock();
			try {
				onPacketAcknowledged.await(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interruption on writing packet", e);
			} finally {
				notifyLock.unlock();
			}
		}

		if (connectionState == ConnectionState.CLOSED) {
			throw new IOException("Socket is closed or reset during write.");
		}

		if (!(payload instanceof StatePayload) || connectionState == ConnectionState.CONNECTING) {
			Lock lock = packetsInFlightLock.writeLock();
			lock.lock();
			try {
				packetsInFlight.add(packet);
			} finally {
				lock.unlock();
			}
		}

		doSend(packet);
	}

	private void doSend(UtpPacket packet) throws IOException {
		// Write the packet
		OutStream outStream = new OutStream();
		packet.write(this, outStream);
		packet.updateSentTime();
		byte[] buffer = outStream.toByteArray();
		lastInteraction = clock.instant();

		utpMultiplexer.send(new DatagramPacket(buffer, buffer.length, socketAddress));
		LOGGER.trace("Sent {} to {} (in flight: {}, {} / {} bytes)", packet, socketAddress, packetsInFlight.size(), getBytesInFlight(), getSendWindowSize());
	}

	public UtpInputStream getInputStream() throws IOException {
		if (inputStream == null) {
			throw new IOException("Socket is not bound.");
		}

		return inputStream;
	}

	public UtpOutputStream getOutputStream() throws IOException {
		if (outputStream == null) {
			throw new IOException("Socket is not bound.");
		}

		return outputStream;
	}

	private int getAvailableWindowSize() {
		return getSendWindowSize() - getBytesInFlight();
	}

	public int getBytesInFlight() {
		Lock lock = packetsInFlightLock.readLock();
		lock.lock();
		try {
			return packetsInFlight.stream().mapToInt(UtpPacket::getPacketSize).sum();
		} finally {
			lock.unlock();
		}
	}

	private int getSendWindowSize() {
		return Math.min(window.getSize(), clientWindowSize);
	}

	public void process(UtpPacket packet) throws IOException {
		// Record the time as early as possible to reduce the noise in the uTP packets.
		int receiveTime = timer.getCurrentMicros();
		lastInteraction = clock.instant();
		LOGGER.trace("Received {} from {}", packet, socketAddress);

		boolean packetCausedConnectedState = false;
		if (connectionState == ConnectionState.CONNECTING && !packetsInFlight.isEmpty()) {
			// We're on the initiating endpoint and thus we are 'connected' once our SYN gets ACK'ed
			setConnectionState(ConnectionState.CONNECTED);
			bindIoStreams(packet.getSequenceNumber());
			packetCausedConnectedState = true;
		}

		clientWindowSize = packet.getWindowSize();

		short oldAcknowledgeNumber = acknowledgeNumber;
		acknowledgeNumber = packet.getSequenceNumber();
		Lock packetLock = packetsInFlightLock.writeLock();
		packetLock.lock();
		try {
			packetsInFlight.stream()
					.filter(packetInFlight -> packetInFlight.getSequenceNumber() == packet.getAcknowledgeNumber())
					.findAny()
					.ifPresent(ackedPacket -> onPacketAcknowledged(receiveTime, packet, ackedPacket));
		} finally {
			packetLock.unlock();
		}
		packet.processPayload(this);

		if (!packetCausedConnectedState && (connectionState != ConnectionState.CONNECTING)) {
			if (oldAcknowledgeNumber != acknowledgeNumber) {
				LOGGER.trace("Sending ACK message for {}.", packet);
				doSend(new UtpPacket(this, new StatePayload()));
			}

			measuredDelay = Math.abs(receiveTime - packet.getTimestampMicroseconds());
		}

		// Notify any waiting threads of the processed packet.
		Sync.signalAll(notifyLock, onPacketAcknowledged);
	}

	private void onPacketAcknowledged(int receiveTime, UtpPacket packet, UtpPacket ackedPacket) {
		if (packet.getTimestampDifferenceMicroseconds() != UNKNOWN_TIMESTAMP_DIFFERENCE) {
			window.update(packet);
			updatePacketSize();
		}

		timeout.update(receiveTime, ackedPacket);

		// Remove the acked packet.
		packetsInFlight.remove(ackedPacket);
		LOGGER.trace("{} message ACKed a packet in flight. (in flight: {}, {} bytes)", packet, packetsInFlight.size(), getBytesInFlight());
	}

	private void updatePacketSize() {
		packetSize = max(150, window.getSize() / 10);
		LOGGER.trace("Packet Size scaled based on new window size: {} bytes", packetSize);
	}

	/**
	 * This method should be triggered when a RESET packet is received.
	 */
	public void onReset() throws IOException {
		if (connectionState == ConnectionState.CLOSED) {
			// Ignore duplicates.
			return;
		}

		LOGGER.debug("Connection got reset.");

		// Create the RESET packet to kill-off the remote.
		UtpPacket resetPacket = new UtpPacket(this, new ResetPayload());

		// Shutdown the connection before sending the packet to reduce the changes on race conditions on duplicate packets.
		setConnectionState(ConnectionState.CLOSED);

		// Send the packet without caring about window restrictions.
		doSend(resetPacket);

		// Stop any waiting threads of sending packets as we don't care about them anymore.
		notifyLock.lock();
		try {
			onPacketAcknowledged.signalAll();
		} finally {
			notifyLock.unlock();
		}

		// Clean the un-acked packets so that the {@link #handleClose()} will clean up this socket one the RESET is ack'ed.
		packetsInFlightLock.writeLock().lock();
		packetsInFlight.clear();
		packetsInFlightLock.writeLock().unlock();

	}

	public void bindIoStreams(short sequenceNumber) {
		if (inputStream != null || outputStream != null) {
			return;
		}
		inputStream = new UtpInputStream(this, (short) (sequenceNumber + 1));
		outputStream = new UtpOutputStream(this);
	}

	/**
	 * Handles the timeout case if one occurred.
	 */
	public void handleTimeout() {
		if (Duration.between(lastInteraction, clock.instant()).minus(timeout.getDuration()).isNegative()) {
			// Timeout has not yet occurred.
			return;
		}

		LOGGER.debug("Socket has encountered a timeout after {}ms.", timeout.getDuration().toMillis());
		packetSize = 150;
		window.onTimeout();
	}

	/**
	 * Handles the period between {@link ConnectionState#DISCONNECTING} and {@link ConnectionState#CLOSED}.
	 */
	public void handleClose() {
		if (!connectionState.isClosedState()) {
			return;
		}

		if (acknowledgeNumber == endOfStreamSequenceNumber && packetsInFlight.isEmpty()) {
			setConnectionState(ConnectionState.CLOSED);
			utpMultiplexer.cleanUpSocket(this);
		}
	}

	/**
	 * Initiates the socket shutdown.
	 * @throws IOException
	 */
	public void close() throws IOException {
		if (connectionState != ConnectionState.CONNECTED) {
			return;
		}

		setConnectionState(ConnectionState.DISCONNECTING);
		send(new FinPayload());
	}

	/**
	 * Verifies if there is still the possibility that more packets are to be received.
	 * @return <code>true</code> when data can still be received, otherwise <code>false</code>.
	 */
	public boolean isInputShutdown() {
		return false;
	}

	/**
	 * Verifies if data can still be written on this socket.
	 * @return <code>true</code> when data can still be written, otherwise <code>false</code>.
	 */
	public boolean isOutputShutdown() {
		return connectionState == ConnectionState.DISCONNECTING || connectionState == ConnectionState.CLOSED;
	}

	public short getAcknowledgeNumber() {
		return acknowledgeNumber;
	}

	public synchronized short nextSequenceNumber() {
		sequenceNumber = sequenceNumberCounter++;
		return sequenceNumber;
	}

	public short getReceivingConnectionId() {
		return connectionIdReceive;
	}

	public short getSendingConnectionId() {
		return connectionIdSend;
	}

	public int getMeasuredDelay() {
		return measuredDelay;
	}

	public int getWindowSize() {
		return utpMultiplexer.getReceiveBufferSize();
	}

	public int getPacketSize() {
		return packetSize - PACKET_OVERHEAD;
	}

	public ConnectionState getConnectionState() {
		return connectionState;
	}

	public void setConnectionState(ConnectionState connectionState) {
		LOGGER.debug("Connection state transitioned from {} to {}", this.connectionState, connectionState);
		this.connectionState = connectionState;

	}

	public void setEndOfStreamSequenceNumber(short sequenceNumber) {
		endOfStreamSequenceNumber = sequenceNumber;
	}

	public short getEndOfStreamSequenceNumber() {
		return endOfStreamSequenceNumber;
	}

	public short getSequenceNumber() {
		return sequenceNumber;
	}

	public static class Builder {

		private UtpMultiplexer utpMultiplexer;
		private SocketAddress socketAddress;

		public Builder setUtpMultiplexer(UtpMultiplexer utpMultiplexer) {
			this.utpMultiplexer = utpMultiplexer;
			return this;
		}

		public Builder setSocketAddress(SocketAddress socketAddress) {
			this.socketAddress = socketAddress;
			return this;
		}

		public UtpSocketImpl build() {
			return new UtpSocketImpl(utpMultiplexer);
		}

		public UtpSocketImpl build(short connectionId) {
			return new UtpSocketImpl(utpMultiplexer, socketAddress, connectionId);
		}

	}
}
