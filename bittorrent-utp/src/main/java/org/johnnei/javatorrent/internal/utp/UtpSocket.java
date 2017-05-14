package org.johnnei.javatorrent.internal.utp;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import org.johnnei.javatorrent.internal.network.socket.ISocket;
import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.packet.Payload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.StatePayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.SynPayload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;
import org.johnnei.javatorrent.internal.utp.stream.PacketWriter;

public class UtpSocket implements ISocket, Closeable {

	private final PacketWriter packetWriter;

	private final PrecisionTimer precisionTimer;

	private final short sendConnectionId;

	private final DatagramChannel channel;

	private short sequenceNumberCounter;

	private ConnectionState connectionState;

	private Queue<UtpPacket> resendQueue;

	private final Queue<Acknowledgement> acknowledgeQueue;

	private short lastSentAcknowledgeNumber;

	private Queue<Payload> sendQueue;

	private PacketAckHandler packetAckHandler;

	/**
	 * Creates a new {@link UtpSocket} and configures it to be the initiating side.
	 * @param channel The channel to write data on.
	 * @param receiveConnectionId The ID on which this socket will receive data.
	 * @return The newly created socket.
	 */
	public static UtpSocket createInitiatingSocket(DatagramChannel channel, short receiveConnectionId) {
		UtpSocket socket = new UtpSocket(channel, receiveConnectionId);
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
		connectionState = ConnectionState.PENDING;
		acknowledgeQueue = new LinkedList<>();
		packetWriter = new PacketWriter();
		precisionTimer = new PrecisionTimer();
	}

	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
		channel.connect(endpoint);
		send(new SynPayload());
		connectionState = ConnectionState.SYN_SENT;
	}

	/**
	 * Updates the socket state based on the received packet.
	 * @param packet The received packet.
	 */
	public void onReceivedPacket(UtpPacket packet) {
		packetAckHandler.onReceivedPacket(packet);
		packet.getPayload().onReceivedPayload(this);
	}

	/**
	 * Submits data to be send.
	 * @param data The buffer to be send.
	 */
	public void send(ByteBuffer data) {
	}

	/**
	 * Submits a packet that has been previously sent but has not arrived on the remote.
	 * @param packet The packet to be resend.
	 */
	public void resend(UtpPacket packet) {
	}

	public void acknowledgePacket(Acknowledgement acknowledgement) {
		acknowledgeQueue.add(acknowledgement);
	}

	/**
	 * Writes the {@link UtpPacket} onto the {@link #channel} if the window allows for it.
	 * This will consume elements from {@link #resendQueue}, {@link #sendQueue} and {@link #packetAckHandler}
	 */
	public void processSendQueue() throws IOException {
		if (!acknowledgeQueue.isEmpty()) {
			send(new StatePayload());
		}
	}

	private void send(Payload payload) throws IOException {
		short ackNumber = lastSentAcknowledgeNumber;
		if (!acknowledgeQueue.isEmpty()) {
			ackNumber = acknowledgeQueue.poll().getSequenceNumber();
			lastSentAcknowledgeNumber = ackNumber;
		}

		UtpHeader header = new UtpHeader.Builder()
			.setType(payload.getType().getTypeField())
			.setSequenceNumber(sequenceNumberCounter++)
			.setExtension((byte) 0)
			.setAcknowledgeNumber(ackNumber)
			.setConnectionId(sendConnectionId)
			.setTimestamp(precisionTimer.getCurrentMicros())
			.setTimestampDifference(0)
			.setWindowSize(64_000)
			.build();
		UtpPacket packet = new UtpPacket(header, payload);
		ByteBuffer buffer = packetWriter.write(packet);
		channel.write(buffer);

		if (buffer.hasRemaining()) {
			throw new IOException("Write buffer utilization exceeded.");
		}
	}


	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return null;
	}

	@Override
	public void close() throws IOException {
		channel.close();
	}

	@Override
	public boolean isClosed() {
		return false;
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
		connectionState = newState;
	}

	public ConnectionState getConnectionState() {
		return connectionState;
	}
}
