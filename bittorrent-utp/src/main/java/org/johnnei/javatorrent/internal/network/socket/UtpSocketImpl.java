package org.johnnei.javatorrent.internal.network.socket;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.johnnei.javatorrent.internal.utils.MathUtils;
import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utp.SlidingTimedValue;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpInputStream;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;
import org.johnnei.javatorrent.internal.utp.protocol.UtpOutputStream;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.internal.utp.protocol.payload.IPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.StatePayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.SynPayload;
import org.johnnei.javatorrent.network.OutStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Integer.max;
import static java.lang.Integer.toUnsignedLong;

/**
 * Internal implementation of the {@link org.johnnei.javatorrent.network.socket.UtpSocket}
 */
public class UtpSocketImpl {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpSocketImpl.class);

	private static final long CONGESTION_CONTROL_TARGET = Duration.of(100, ChronoUnit.MILLIS).toNanos() / 1000;

	private static final int MAX_WINDOW_CHANGE_PER_PACKET = 100;

	private static final int PACKET_OVERHEAD = 20;

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

	private Duration timeout;

	private Instant lastInteraction;

	private int rtt;

	private int rttVariance;

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

	private int maxWindow;

	private UtpInputStream inputStream;

	private UtpOutputStream outputStream;

	private short sequenceNumber;

	private int measuredDelay;

	private SlidingTimedValue<Integer> slidingBaseDelay;

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
		timeout = Duration.of(1000, ChronoUnit.MILLIS);
		packetSize = 150;
		maxWindow = 150;
		clientWindowSize = 150;
		slidingBaseDelay = new SlidingTimedValue<>();
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
		while (getAvailableWindowSize() < packet.getPacketSize()) {
			LOGGER.trace("Waiting to send packet of {} bytes. Window Status: {} / {} bytes", packet.getPacketSize(), getBytesInFlight(), getSendWindowSize());
			notifyLock.lock();
			try {
				onPacketAcknowledged.await(1, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				throw new IOException("Interruption on writing packet", e);
			} finally {
				notifyLock.unlock();
			}
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

	private int getBytesInFlight() {
		Lock lock = packetsInFlightLock.readLock();
		lock.lock();
		try {
			return packetsInFlight.stream().mapToInt(UtpPacket::getPacketSize).sum();
		} finally {
			lock.unlock();
		}
	}

	private int getSendWindowSize() {
		return Math.min(maxWindow, clientWindowSize);
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
					.ifPresent(ackedPacket -> {

						if (packet.getTimestampDifferenceMicroseconds() != 0) {
							slidingBaseDelay.addValue(packet.getTimestampDifferenceMicroseconds());
							long ourDelay = toUnsignedLong(packet.getTimestampDifferenceMicroseconds()) - toUnsignedLong(slidingBaseDelay.getMinimum());
							long offTarget = CONGESTION_CONTROL_TARGET - ourDelay;
							double delayFactor = MathUtils.clamp(-1, 1, offTarget / (double) CONGESTION_CONTROL_TARGET);
							double windowFactor = MathUtils.clamp(-1, 1, getBytesInFlight() / (double) maxWindow);
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

							packetSize = max(150, maxWindow / 10);
							LOGGER.trace("Packet Size scaled based on new window size: {} bytes", packetSize);
						}

						// Calculate RTT and RTT Variance, and update the timeout value accordingly.
						int packetRtt = (receiveTime - ackedPacket.getSentTime());
						int delta = rtt - packetRtt;
						rttVariance += (Math.abs(delta) - rttVariance) / 4;
						rtt += (packetRtt - rtt) / 8;
						// Divide by 1000 to make the RTT measurements into millis.
						timeout = Duration.of(max((rtt + rttVariance * 4) / 1000, 500), ChronoUnit.MILLIS);

						// Remove the acked packet.
						packetsInFlight.remove(ackedPacket);
						LOGGER.trace("{} message ACKed a packet in flight. (in flight: {}, {} bytes)", packet, packetsInFlight.size(), getBytesInFlight());
					});
		} finally {
			packetLock.unlock();
		}
		packet.processPayload(this);

		if (!packetCausedConnectedState && connectionState == ConnectionState.CONNECTED) {
			if (oldAcknowledgeNumber != acknowledgeNumber) {
				LOGGER.trace("Sending ACK message for {}.", packet);
				doSend(new UtpPacket(this, new StatePayload()));
			}

			measuredDelay = Math.abs(receiveTime - packet.getTimestampMicroseconds());
		}

		// Notify any waiting threads of the processed packet.
		notifyLock.lock();
		try {
			onPacketAcknowledged.signalAll();
		} finally {
			notifyLock.unlock();
		}

	}

	public void bindIoStreams(short sequenceNumber) {
		if (inputStream != null || outputStream != null) {
			return;
		}
		inputStream = new UtpInputStream((short) (sequenceNumber + 1));
		outputStream = new UtpOutputStream(this);
	}

	/**
	 * Handles the timeout case if one occurred.
	 */
	public void handleTimeout() {
		if (Duration.between(lastInteraction, clock.instant()).minus(timeout).isNegative()) {
			// Timeout has not yet occurred.
			return;
		}

		LOGGER.debug("Socket has encountered a timeout after {}ms.", timeout.toMillis());
		packetSize = 150;
		// TODO Make this neater, according to the spec this should SET to 150 as it will allow one more packet to be sent.
		// This implementation won't do that though. So maybe I think of maxWindow incorrectly?
		maxWindow += 150;
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
