package org.johnnei.javatorrent.internal.network.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.sql.Date;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utils.Sync;
import org.johnnei.javatorrent.internal.utp.UtpTimeout;
import org.johnnei.javatorrent.internal.utp.UtpWindow;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpAckHandler;
import org.johnnei.javatorrent.internal.utp.protocol.UtpInputStream;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;
import org.johnnei.javatorrent.internal.utp.protocol.UtpOutputStream;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.UtpProtocol;
import org.johnnei.javatorrent.internal.utp.protocol.payload.DataPayload;
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
 * Internal implementation of the {@link org.johnnei.javatorrent.internal.network.socket.UtpSocket}
 */
public class UtpSocketImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpSocketImpl.class);

	private static final int PACKET_OVERHEAD = 20;
	private static final int UNKNOWN_TIMESTAMP_DIFFERENCE = 0;

	private final Lock notifyLock = new ReentrantLock();

	/**
	 * A condition which gets triggered when a packet is acknowledged and thus making room in the write queue
	 */
	private final Condition onPacketAcknowledged = notifyLock.newCondition();

	private final UtpMultiplexer utpMultiplexer;

	private Clock clock = Clock.systemDefaultZone();

	private PrecisionTimer timer = new PrecisionTimer();

	private SocketAddress socketAddress;

	private ConnectionState connectionState;

	private UtpAckHandler ackHandler;

	private Instant lastInteraction;

	private UtpTimeout timeout;

	/**
	 * The sequence number which as marked as the last packet by {@link org.johnnei.javatorrent.internal.utp.protocol.payload.FinPayload}
	 */
	private short endOfStreamSequenceNumber;

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

	private AtomicInteger statePackets = new AtomicInteger(0);

	private AtomicInteger dataPackets = new AtomicInteger(0);

	/**
	 * Creates a new socket which is considered the initiating endpoint
	 *
	 * @param utpMultiplexer The multiplexer on which this socket will be registered.
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
	 *
	 * @param utpMultiplexer The multiplexer on which this socket will be registered.
	 * @param socketAddress The socket address on which the remote end is listening.
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
		packetSize = 150;
		clientWindowSize = 150;
		timeout = new UtpTimeout();
		window = new UtpWindow(this);
		lastInteraction = clock.instant();
		ackHandler = new UtpAckHandler(this);
	}

	/**
	 * Attempts to connect the socket to the given endpoint.
	 *
	 * @param endpoint The address on which a client is expected to be listening.
	 * @throws IOException When the connection cannot be established.
	 */
	public void connect(InetSocketAddress endpoint) throws IOException {
		socketAddress = endpoint;
		connectionState = ConnectionState.CONNECTING;
		send(new SynPayload());

		// Calculate the time until we'll be waiting for our syn packet to get ACK'ed.
		Instant endTime = clock.instant().plusSeconds(10);

		// Wait for the acknowledgement of the connection.
		notifyLock.lock();
		try {
			while (connectionState != ConnectionState.CONNECTED && clock.instant().isBefore(endTime)) {
				onPacketAcknowledged.awaitUntil(Date.from(endTime));
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Interruption while waiting for connection confirmation.", e);
		} finally {
			notifyLock.unlock();
		}

		if (connectionState != ConnectionState.CONNECTED) {
			throw new IOException("Endpoint did not respond to connection attempt");
		}
	}

	/**
	 * Writes the given payload with respect to the {@link #window}.
	 *
	 * @param payload The payload to send.
	 * @throws IOException When an IO error occurs.
	 */
	public void send(IPayload payload) throws IOException {
		send(new UtpPacket(this, payload));
	}

	private void send(UtpPacket packet) throws IOException {
		Instant sendTimeoutTime = clock.instant().plusSeconds(10);

		// Wait for enough space to write the packet, ST_STATE packets are allowed to by-pass this.
		while (connectionState != ConnectionState.CLOSED && getAvailableWindowSize() < packet.getPacketSize()) {
			LOGGER.trace("Waiting to send packet (seq={}) of {} bytes. Window Status: {} / {} bytes",
					Short.toUnsignedInt(packet.getSequenceNumber()),
					packet.getPacketSize(),
					getBytesInFlight(),
					getSendWindowSize());

			if (mustRepackagePayload(packet)) {
				repackageAndSendPayload(packet);
				return;
			}

			notifyLock.lock();
			try {
				if (!onPacketAcknowledged.awaitUntil(java.util.Date.from(sendTimeoutTime))) {
					throw new SocketTimeoutException("Failed to send packet in a reasonable time frame.");
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IOException("Interruption on writing packet", e);
			} finally {
				notifyLock.unlock();
			}
		}

		sendUnbounded(packet);
	}

	private boolean mustRepackagePayload(UtpPacket packet) {
		// Packet exceed the maximum window, this will never get send. Repackage it.
		return packet.getPacketSize() > Math.max(150, window.getSize());
	}

	private void repackageAndSendPayload(UtpPacket packet) throws IOException {
		LOGGER.trace("Repacking {} it exceeds the maximum window size of {}", packet, window.getSize());
		byte[] unsentBytes = packet.repackage(this);
		// Keep data integrity order.
		send(packet);
		send(new DataPayload(unsentBytes));
	}

	/**
	 * Send a packet without honoring the {@link #window}.
	 *
	 * @param packet The packet
	 * @see #send(IPayload)
	 */
	public void sendUnbounded(UtpPacket packet) throws IOException {
		if (connectionState == ConnectionState.CLOSED) {
			throw new IOException("Socket is closed or reset during write.");
		}

		ackHandler.registerPacket(packet);

		doSend(packet);
	}

	/**
	 * Called when a {@link org.johnnei.javatorrent.internal.utp.protocol.payload.DataPayload} has been received.
	 */
	public void onReceivedData() {
		if (connectionState == ConnectionState.CONNECTING && ackHandler.isInitialised()) {
			LOGGER.debug("Data packet has been received after SYN ack, connection is ok.");
			setConnectionState(ConnectionState.CONNECTED);
		}
	}

	private void doSend(UtpPacket packet) throws IOException {
		// Write the packet
		OutStream outStream = new OutStream();
		packet.write(this, outStream);
		packet.updateSentTime();
		byte[] buffer = outStream.toByteArray();
		lastInteraction = clock.instant();

		utpMultiplexer.send(new DatagramPacket(buffer, buffer.length, socketAddress));
		LOGGER.trace("Sent {} to {} ({} / {} bytes)", packet, socketAddress, ackHandler, getSendWindowSize());

		if (packet.getType() == UtpProtocol.ST_STATE) {
			statePackets.incrementAndGet();
		} else if (packet.getType() == UtpProtocol.ST_DATA) {
			dataPackets.incrementAndGet();
		}
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
		return ackHandler.countBytesInFlight();
	}

	private int getSendWindowSize() {
		return Math.min(window.getSize(), clientWindowSize);
	}

	/**
	 * Processes the payload of the given <code>packet</code> and updates the socket state accordingly.
	 *
	 * @param packet The packet to process.
	 * @throws IOException When the payload cannot be processed.
	 */
	public void process(UtpPacket packet) throws IOException {
		// Record the time as early as possible to reduce the noise in the uTP packets.
		int receiveTime = timer.getCurrentMicros();
		lastInteraction = clock.instant();
		LOGGER.trace("Received {} from {}", packet, socketAddress);

		clientWindowSize = packet.getWindowSize();

		Optional<UtpPacket> ackedPacket = ackHandler.onReceivedPacket(packet);
		ackedPacket.ifPresent(p -> {
			onPacketAcknowledged(receiveTime, packet, p);
			measuredDelay = Math.abs(receiveTime - packet.getTimestampMicroseconds());
		});
		packet.processPayload(this);

		// Notify any waiting threads of the processed packet.
		Sync.signalAll(notifyLock, onPacketAcknowledged);
	}

	private void onPacketAcknowledged(int receiveTime, UtpPacket packet, UtpPacket ackedPacket) {
		LOGGER.trace("{} ACKed {} which was in flight. ({})", packet, ackedPacket, ackHandler);
		if (packet.getTimestampDifferenceMicroseconds() != UNKNOWN_TIMESTAMP_DIFFERENCE) {
			window.update(packet);
			updatePacketSize();
		}

		if (ackedPacket.getType() == UtpProtocol.ST_SYN) {
			LOGGER.debug("Received ACK on SYN, connection is ok.");
			setConnectionState(ConnectionState.CONNECTED);
		}

		timeout.update(receiveTime, ackedPacket);
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
		ackHandler.onReset();
	}

	/**
	 * Initialises the input and output stream of this socket.
	 *
	 * @param sequenceNumber The sequence number of the packet which is the last packet of the connection establishment.
	 * @see #getInputStream()
	 * @see #getOutputStream()
	 */
	public void bindIoStreams(short sequenceNumber) {
		inputStream = new UtpInputStream(this, (short) (sequenceNumber + 1));
		outputStream = new UtpOutputStream(this);
	}

	/**
	 * Handles the timeout case if one occurred.
	 */
	public void handleTimeout() throws IOException {
		if (Duration.between(lastInteraction, clock.instant()).minus(timeout.getDuration()).isNegative()) {
			// Timeout has not yet occurred.
			return;
		}

		int statePacketCount = statePackets.getAndSet(0);
		int dataPacketCount = dataPackets.getAndSet(0);

		LOGGER.debug("Socket has encountered a timeout after {}ms. Send Packets: {} Data, {} State.",
				timeout.getDuration().toMillis(),
				dataPacketCount,
				statePacketCount);
		packetSize = 150;
		window.onTimeout();

		sendUnbounded(new UtpPacket(this, new StatePayload()));
	}

	/**
	 * Handles the period between {@link ConnectionState#DISCONNECTING} and {@link ConnectionState#CLOSED}.
	 */
	public void handleClose() {
		if (!connectionState.isClosedState()) {
			return;
		}

		if (getAcknowledgeNumber() == endOfStreamSequenceNumber && !ackHandler.hasPacketsInFlight()) {
			setConnectionState(ConnectionState.CLOSED);
			utpMultiplexer.cleanUpSocket(this);
		}
	}

	/**
	 * Initiates the socket shutdown.
	 *
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
	 *
	 * @return <code>true</code> when data can still be received, otherwise <code>false</code>.
	 */
	public boolean isInputShutdown() {
		return false;
	}

	/**
	 * Verifies if data can still be written on this socket.
	 *
	 * @return <code>true</code> when data can still be written, otherwise <code>false</code>.
	 */
	public boolean isOutputShutdown() {
		return connectionState == ConnectionState.DISCONNECTING || connectionState == ConnectionState.CLOSED;
	}

	public short getAcknowledgeNumber() {
		return ackHandler.getAcknowledgeNumber();
	}

	/**
	 * @return The next unique sequence number to use to send out a packet.
	 */
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

	void setConnectionState(ConnectionState connectionState) {
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

	@Override
	public String toString() {
		return String.format("UtpSocketImpl[state=%s, window=%s, timeout=%sms, ackHandler=%s]",
				connectionState,
				window.getSize(),
				timeout.getDuration().toMillis(),
				ackHandler);
	}

	/**
	 * The class capable of building {@link UtpSocketImpl} instances progressively.
	 */
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

		/**
		 * @return A new socket which is considered to connect to an endpoint.
		 */
		public UtpSocketImpl build() {
			return new UtpSocketImpl(utpMultiplexer);
		}

		/**
		 * @param connectionId The ID of the connection on which we'll send out.
		 * @return A new socket which is considered the receiving endpoint.
		 */
		public UtpSocketImpl build(short connectionId) {
			return new UtpSocketImpl(utpMultiplexer, socketAddress, connectionId);
		}

	}
}
