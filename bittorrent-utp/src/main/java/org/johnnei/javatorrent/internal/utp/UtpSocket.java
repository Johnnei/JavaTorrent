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
import org.johnnei.javatorrent.internal.utp.protocol.packet.Payload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.StatePayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.SynPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;
import org.johnnei.javatorrent.internal.utp.stream.PacketWriter;
import org.johnnei.javatorrent.internal.utp.stream.SocketTimeoutHandler;
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

	private short sequenceNumberCounter;

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

	/**
	 * Creates a new {@link UtpSocket} and configures it to be the initiating side.
	 *
	 * @param channel The channel to write data on.
	 * @param receiveConnectionId The ID on which this socket will receive data.
	 * @return The newly created socket.
	 */
	public static UtpSocket createInitiatingSocket(DatagramChannel channel, short receiveConnectionId) {
		UtpSocket socket = new UtpSocket(channel, (short) (receiveConnectionId + 1));
		socket.sequenceNumberCounter = 1;
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
			windowHandler.onReceivedPacket(packet);
			// TODO When packet in flight is ack'ed update the timeout (JBT-65)
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
	}

	public void acknowledgePacket(Acknowledgement acknowledgement) {
		acknowledgeQueue.add(acknowledgement);
	}

	/**
	 * Writes the {@link UtpPacket} onto the {@link #channel} if the window allows for it.
	 * This will consume elements from {@link #resendQueue}, {@link #sendQueue} and {@link #packetAckHandler}
	 */
	public void processSendQueue() throws IOException {
		if (!resendQueue.isEmpty()) {
			send(resendQueue.poll());
		} else {
			int maxPayloadSize = windowHandler.getMaxWindow() - windowHandler.getBytesInFlight() - PacketWriter.OVERHEAD_IN_BYTES;
			if (!sendQueue.isEmpty() && sendQueue.peek().getData().length <= maxPayloadSize) {
				send(sendQueue.poll());
			} else if (!acknowledgeQueue.isEmpty() && maxPayloadSize >= 0) {
				send(new StatePayload());
			}
		}
	}

	/**
	 * Validates if the socket is in timeout state or not.
	 */
	public void processTimeout() {
		if (!timeoutHandler.isTimeoutExpired()) {
			return;
		}

		timeoutHandler.onTimeout();
		// FIXME: Set packet size to 150 (JBT-73)
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
		send(packet);
	}

	private void send(UtpPacket packet) throws IOException {
		short ackNumber = lastSentAcknowledgeNumber;
		if (!acknowledgeQueue.isEmpty()) {
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
			return sequenceNumberCounter++;
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
		// TODO Adhere packet sizing (JBT-73)
		return 150 - PacketWriter.OVERHEAD_IN_BYTES;
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
	public void close() throws IOException {
		// TODO Implement closing of socket (JBT-69).
	}

	public void submitData(short sequenceNumber, byte[] data) {
		inputStream.submitData(sequenceNumber, data);
	}

	@Override
	public boolean isClosed() {
		return connectionState == ConnectionState.PENDING;
	}

	@Override
	public boolean isInputShutdown() {
		return false;
	}

	@Override
	public boolean isOutputShutdown() {
		return false;
	}

	@Override
	public void flush() throws IOException {
	}

	public void setConnectionState(ConnectionState newState) {
		// FIXME Check if transition is allowed.
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
