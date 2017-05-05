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
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.packet.Payload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

public class UtpSocket implements ISocket, Closeable {

	private final int sendConnectionId;

	private final int receiveConnectionId;

	private final DatagramChannel channel;

	private short sequenceNumberCounter;

	private ConnectionState connectionState;

	private Queue<UtpPacket> resendQueue;

	private final Queue<Acknowledgement> acknowledgeQueue;

	private Queue<Payload> sendQueue;

	private PacketAckHandler packetAckHandler;

	public static UtpSocket createRemoteConnecting(DatagramChannel channel, UtpPacket synPacket) {
		short sendConnectionId = synPacket.getHeader().getConnectionId();
		UtpSocket socket = new UtpSocket(channel, (short) (sendConnectionId + 1), sendConnectionId);
		socket.connectionState = ConnectionState.SYN_RECEIVED;
		socket.sequenceNumberCounter = (short) new Random().nextInt();
		socket.packetAckHandler = new PacketAckHandler(socket, (short) (synPacket.getHeader().getSequenceNumber() - 1));
		return socket;
	}

	private UtpSocket(DatagramChannel channel, short receiveConnectionId, short sendConnectionId) {
		this.channel = channel;
		this.receiveConnectionId = receiveConnectionId;
		this.sendConnectionId = sendConnectionId;
		acknowledgeQueue = new LinkedList<>();
	}

	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
	}

	/**
	 * Updates the socket state based on the received packet.
	 * @param packet The received packet.
	 */
	public void onReceivedPacket(UtpPacket packet) {
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
	public void processSendQueue() {
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

	public ConnectionState getConnectionState() {
		return connectionState;
	}
}
