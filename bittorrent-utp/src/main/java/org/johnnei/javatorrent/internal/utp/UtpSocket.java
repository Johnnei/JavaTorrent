package org.johnnei.javatorrent.internal.utp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.johnnei.javatorrent.internal.network.socket.ISocket;
import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utils.Sync;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.packet.DataPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.FinPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.Payload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.StatePayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.SynPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;
import org.johnnei.javatorrent.internal.utp.stream.PacketWriter;
import org.johnnei.javatorrent.internal.utp.stream.StreamState;
import org.johnnei.javatorrent.internal.utp.stream.UtpInputStream;
import org.johnnei.javatorrent.internal.utp.stream.UtpOutputStream;

import static org.johnnei.javatorrent.internal.utp.protocol.PacketType.STATE;
import static org.johnnei.javatorrent.internal.utp.protocol.PacketType.SYN;

public class UtpSocket implements ISocket, Closeable {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpSocket.class);

	private final Lock notifyLock = new ReentrantLock();

	private final Condition onStateChange = notifyLock.newCondition();

	private final Clock clock;

	private final PacketWriter packetWriter;

	private final PrecisionTimer precisionTimer;

	private final short sendConnectionId;

	private final DatagramChannel channel;

	/**
	 * The time which needs to expire until we are allowed to violate the window as defined by the spec.
	 */
	private Instant nextWindowViolation;

	private short sequenceNumberCounter;

	private Short endOfStreamSequenceNumber;

	private ConnectionState connectionState;

	private final Queue<UtpPacket> resendQueue;

	private final Queue<Acknowledgement> acknowledgeQueue;

	private short lastSentAcknowledgeNumber;

	private final Queue<Payload> sendQueue;

	private PacketAckHandler packetAckHandler;

	private UtpOutputStream outputStream;

	private UtpInputStream inputStream;

	private SocketAddress remoteAddress;

	private SocketTimeoutHandler timeoutHandler;

	private PacketLossHandler packetLossHandler;

	private SocketWindowHandler windowHandler;

	private PacketSizeHandler packetSizeHandler;

	private StreamState outputStreamState;

	private StreamState inputStreamState;

	/**
	 * Creates a new {@link UtpSocket} and configures it to be the initiating side.
	 *
	 * @param channel The channel to write data on.
	 * @param receiveConnectionId The ID on which this socket will receive data.
	 * @return The newly created socket.
	 */
	public static UtpSocket createInitiatingSocket(DatagramChannel channel, short receiveConnectionId) {
		UtpSocket socket = new UtpSocket(channel, (short) (receiveConnectionId + 1));
		socket.sequenceNumberCounter = 0;
		socket.packetAckHandler = new PacketAckHandler(socket);
		return socket;
	}

	public static UtpSocket createRemoteConnecting(DatagramChannel channel, UtpPacket synPacket) {
		short sendConnectionId = synPacket.getHeader().getConnectionId();
		UtpSocket socket = new UtpSocket(channel, sendConnectionId);
		socket.sequenceNumberCounter = (short) new Random().nextInt();
		socket.packetAckHandler = new PacketAckHandler(socket, (short) (synPacket.getHeader().getSequenceNumber() - 1));
		return socket;
	}

	private UtpSocket(DatagramChannel channel, short sendConnectionId) {
		this.channel = channel;
		this.sendConnectionId = sendConnectionId;
		clock = Clock.systemDefaultZone();
		connectionState = ConnectionState.PENDING;
		acknowledgeQueue = new LinkedList<>();
		sendQueue = new LinkedList<>();
		resendQueue = new LinkedList<>();
		packetWriter = new PacketWriter();
		precisionTimer = new PrecisionTimer();
		outputStream = new UtpOutputStream(this);
		timeoutHandler = new SocketTimeoutHandler(precisionTimer);
		packetLossHandler = new PacketLossHandler(this);
		windowHandler = new SocketWindowHandler();
		packetSizeHandler = new PacketSizeHandler(windowHandler);
		inputStreamState = StreamState.ACTIVE;
		outputStreamState = StreamState.ACTIVE;
		nextWindowViolation = clock.instant();
	}

	public void bind(SocketAddress remoteAddress) {
		this.remoteAddress = remoteAddress;
	}

	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
		bind(endpoint);
		send(new SynPayload());
		connectionState = ConnectionState.SYN_SENT;

		Date timeout = Date.from(clock.instant().plusSeconds(10));
		notifyLock.lock();
		try {
			while (connectionState != ConnectionState.CONNECTED) {
				if (!onStateChange.awaitUntil(timeout)) {
					throw new IOException("Connection was not accepted within timeout.");
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IOException("Connecting got interrupted.", e);
		} finally {
			notifyLock.unlock();
		}
	}

	/**
	 * Updates the socket state based on the received packet.
	 *
	 * @param packet The received packet.
	 */
	public void onReceivedPacket(UtpPacket packet) {
		try (MDC.MDCCloseable ignored = MDC.putCloseable("context", Integer.toString(Short.toUnsignedInt(sendConnectionId)))) {
			LOGGER.trace(
				"Received [{}] packet [{}]",
				PacketType.getByType(packet.getHeader().getType()),
				Short.toUnsignedInt(packet.getHeader().getSequenceNumber())
			);

			packetAckHandler.onReceivedPacket(packet);
			packetLossHandler.onReceivedPacket(packet);
			if (windowHandler.onReceivedPacket(packet)) {
				timeoutHandler.onReceivedPacket(packet);
			}
			packetSizeHandler.onReceivedPacket(packet);
			packet.getPayload().onReceivedPayload(packet.getHeader(), this);
		}
	}

	/**
	 * Submits data to be send.
	 *
	 * @param data The buffer to be send.
	 */
	public void send(ByteBuffer data) {
		sendQueue.add(new DataPayload(data));
	}

	/**
	 * Submits a packet that has been previously sent but has not arrived on the remote.
	 *
	 * @param packet The packet to be resend.
	 */
	public void resend(UtpPacket packet) {
		resendQueue.add(packet);
		windowHandler.onPacketLoss(packet);
		packetSizeHandler.onPacketLoss();
	}

	public void acknowledgePacket(Acknowledgement acknowledgement) {
		acknowledgeQueue.add(acknowledgement);
	}

	/**
	 * Writes the {@link UtpPacket} onto the {@link #channel} if the window allows for it.
	 * This will consume elements from {@link #resendQueue}, {@link #sendQueue} and {@link #packetAckHandler}
	 */
	public void processSendQueue() throws IOException {
		boolean canSendMultiple;
		do {
			canSendMultiple = false;
			if (!resendQueue.isEmpty()) {
				send(resendQueue.poll(), false);
				canSendMultiple = true;
			} else {
				int maxPayloadSize = windowHandler.getMaxWindow() - windowHandler.getBytesInFlight() - PacketWriter.OVERHEAD_IN_BYTES;
				if (canSendNewPacket(maxPayloadSize)) {
					send(sendQueue.poll());
					canSendMultiple = true;
				} else if (sendQueue.isEmpty() && outputStreamState == StreamState.SHUTDOWN_PENDING) {
					send(new FinPayload());
					outputStreamState = StreamState.SHUTDOWN;
				} else if (!acknowledgeQueue.isEmpty() && maxPayloadSize >= 0) {
					send(new StatePayload());
				}
			}
		} while (canSendMultiple);
	}

	private boolean canSendNewPacket(int maxPayloadSize) {
		if (sendQueue.isEmpty()) {
			return false;
		}

		int payloadSize = sendQueue.peek().getData().length;

		if (payloadSize <= maxPayloadSize) {
			return true;
		}

		// Consider window violation for packets which are exceeding the max window size and thus will block the entire buffer.
		int maxWindow = windowHandler.getMaxWindow();
		if (payloadSize > maxWindow && maxWindow > 0 && clock.instant().isAfter(nextWindowViolation)) {
			int secondsToAdd = payloadSize / maxWindow;
			nextWindowViolation = clock.instant().plusSeconds(secondsToAdd);
			LOGGER.trace(
				"Violating window of [{}] bytes by [{}] bytes (shortage: [{}]). Blocking exceeds for [{}] seconds",
				maxWindow,
				payloadSize,
				payloadSize - maxWindow,
				secondsToAdd
			);
			return true;
		}

		return false;
	}

	/**
	 * Validates if the socket is in timeout state or not.
	 */
	public void processTimeout() {
		if (!timeoutHandler.isTimeoutExpired()) {
			return;
		}

		try (MDC.MDCCloseable ignored = MDC.putCloseable("context", Integer.toString(Short.toUnsignedInt(sendConnectionId)))) {
			LOGGER.trace(
				"Socket triggered timeout. Window: {} bytes. Bytes in flight: {}. Payload Size: {} bytes. Resend Queue: {} packets. Send Queue: {} packets (head: {}), Ack Queue: {} packets.",
				windowHandler.getMaxWindow(),
				windowHandler.getBytesInFlight(),
				getPacketPayloadSize(),
				resendQueue.size(),
				sendQueue.size(),
				sendQueue.peek(),
				acknowledgeQueue.size()
			);
		}

		timeoutHandler.onTimeout();
		packetSizeHandler.onTimeout();
		windowHandler.onTimeout();
	}

	private void send(Payload payload) throws IOException {
		UtpHeader header = new UtpHeader.Builder()
			.setType(payload.getType().getTypeField())
			.setSequenceNumber(getPacketSequenceNumber(payload.getType()))
			.setExtension((byte) 0)
			.setConnectionId(getSendConnectionId(payload.getType()))
			.setWindowSize(windowHandler.getBytesInFlight())
			.build();
		UtpPacket packet = new UtpPacket(header, payload);
		send(packet, true);
	}

	private void send(UtpPacket packet, boolean renewAck) throws IOException {
		short ackNumber = lastSentAcknowledgeNumber;
		if (!renewAck) {
			ackNumber = packet.getHeader().getAcknowledgeNumber();
		} else if (!acknowledgeQueue.isEmpty()) {
			ackNumber = acknowledgeQueue.poll().getSequenceNumber();
			lastSentAcknowledgeNumber = ackNumber;
		}

		packet.getHeader().renew(ackNumber, precisionTimer.getCurrentMicros(), 0);

		ByteBuffer buffer = packetWriter.write(packet);

		try (MDC.MDCCloseable ignored = MDC.putCloseable("context", Integer.toString(Short.toUnsignedInt(sendConnectionId)))) {
			LOGGER.trace(
				"Writing [{}] packet [{}] acking [{}] of [{}] bytes",
				PacketType.getByType(packet.getHeader().getType()),
				Short.toUnsignedInt(packet.getHeader().getSequenceNumber()),
				Short.toUnsignedInt(packet.getHeader().getAcknowledgeNumber()),
				buffer.limit()
			);

			channel.send(buffer, remoteAddress);
			packetLossHandler.onSentPacket(packet);
			timeoutHandler.onSentPacket();
			windowHandler.onSentPacket(packet);
			packetSizeHandler.onSentPacket(packet);
		}

		if (buffer.hasRemaining()) {
			throw new IOException("Write buffer utilization exceeded.");
		}
	}

	private short getPacketSequenceNumber(PacketType type) {
		// The state packet acking the SYN _DOES_ increase the sequence number.
		if (type == STATE && connectionState != ConnectionState.SYN_RECEIVED) {
			return sequenceNumberCounter;
		} else {
			return ++sequenceNumberCounter;
		}
	}

	private short getSendConnectionId(PacketType type) {
		if (type == SYN) {
			return (short) (sendConnectionId - 1);
		} else {
			return sendConnectionId;
		}
	}

	/**
	 * @return The amount of bytes the payload currently holds when transmitting a packet.
	 */
	public int getPacketPayloadSize() {
		return packetSizeHandler.getPacketSize() - PacketWriter.OVERHEAD_IN_BYTES;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return Objects.requireNonNull(inputStream, "Connection was not established yet");
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return outputStream;
	}

	@Override
	public void close() {
		flush();
		setConnectionState(ConnectionState.CLOSING);
		outputStreamState = StreamState.SHUTDOWN_PENDING;
	}

	public void submitData(short sequenceNumber, byte[] data) {
		inputStream.submitData(sequenceNumber, data);
	}

	public void shutdownInputStream(short sequenceNumber) {
		endOfStreamSequenceNumber = sequenceNumber;
		inputStreamState = StreamState.SHUTDOWN;
		setConnectionState(ConnectionState.CLOSING);
		if (outputStreamState == StreamState.ACTIVE) {
			close();
		}
	}

	@Override
	public boolean isClosed() {
		return connectionState == ConnectionState.PENDING || connectionState == ConnectionState.CLOSED || connectionState == ConnectionState.RESET;
	}

	@Override
	public boolean isInputShutdown() {
		return inputStreamState != StreamState.ACTIVE;
	}

	@Override
	public boolean isOutputShutdown() {
		return outputStreamState != StreamState.ACTIVE;
	}

	@Override
	public void flush() {
		outputStream.flush();
	}

	public boolean isShutdown() {
		return connectionState == ConnectionState.RESET || (inputStreamState == StreamState.SHUTDOWN
			&& inputStream.isCompleteUntil(endOfStreamSequenceNumber)
			&& outputStreamState == StreamState.SHUTDOWN
			&& windowHandler.getBytesInFlight() == 0);
	}

	public void setConnectionState(ConnectionState newState) {
		try (MDC.MDCCloseable ignored = MDC.putCloseable("context", Integer.toString(Short.toUnsignedInt(sendConnectionId)))) {
			LOGGER.trace(
				"Transitioning state from {} to {}",
				connectionState,
				newState
			);
		}
		connectionState = newState;

		if (connectionState == ConnectionState.CONNECTED) {
			inputStream = new UtpInputStream((short) (lastSentAcknowledgeNumber + 1));
		}

		Sync.signalAll(notifyLock, onStateChange);
	}

	public ConnectionState getConnectionState() {
		return connectionState;
	}

	public void setAcknowledgeNumber(short acknowledgeNumber) {
		this.lastSentAcknowledgeNumber = acknowledgeNumber;
	}

	@Override
	public String toString() {
		return String.format("UtpSocket[sendConnectionId=%s, remote=%s]", Short.toUnsignedInt(sendConnectionId), remoteAddress);
	}
}
