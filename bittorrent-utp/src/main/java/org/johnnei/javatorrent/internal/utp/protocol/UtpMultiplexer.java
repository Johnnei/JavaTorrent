package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.internal.network.UtpPeerConnectionAcceptor;
import org.johnnei.javatorrent.internal.network.socket.UtpSocket;
import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.UtpSocketRegistration;
import org.johnnei.javatorrent.internal.utp.protocol.payload.UtpPayloadFactory;
import org.johnnei.javatorrent.module.ModuleBuildException;
import org.johnnei.javatorrent.network.InStream;

/**
 * This class is capable of multiplexing all {@link UtpSocket} instances on a single {@link DatagramSocket} and forwarding the packets to the sockets.
 */
public class UtpMultiplexer implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpMultiplexer.class);

	private final Object socketListLock = new Object();

	private TorrentClient torrentClient;

	private UtpPeerConnectionAcceptor connectionAcceptor;

	private LoopingRunnable connectionAcceptorRunnable;

	private UtpSocketImpl.Builder utpSocketFactory;

	/**
	 * The Factory to create the packet instances<br>
	 * If JavaTorrent will need to update the protocol then we can use multiple factory's to create the correct version of the packet
	 */
	private UtpPayloadFactory packetFactory;

	/**
	 * The socket on which the udp packet will be received and send
	 */
	DatagramSocket multiplexerSocket;

	/**
	 * All {@link UtpSocket}s which have registered to listen for packets
	 */
	private Map<Short, UtpSocketRegistration> utpSockets;

	private int receiveBufferSize;

	Thread connectionAcceptorThread;

	/**
	 * Creates a new multiplexer based on the given <code>torrentClient</code>
	 * @param torrentClient The torrent client containing the desired configuration.
	 * @throws ModuleBuildException When the datagram socket cannot be bound.
	 */
	public UtpMultiplexer(TorrentClient torrentClient) throws ModuleBuildException {
		this.torrentClient = torrentClient;
		utpSockets = new HashMap<>();
		utpSocketFactory = new UtpSocketImpl.Builder()
				.setUtpMultiplexer(this);
		packetFactory = new UtpPayloadFactory();
		connectionAcceptor = new UtpPeerConnectionAcceptor(torrentClient);
		connectionAcceptorRunnable = new LoopingRunnable(connectionAcceptor);
		startMultiplexer(torrentClient.getDownloadPort());
	}

	void startMultiplexer(int port) throws ModuleBuildException {
		try {
			multiplexerSocket = new DatagramSocket(port);
			receiveBufferSize = multiplexerSocket.getReceiveBufferSize();
		} catch (IOException e) {
			throw new ModuleBuildException("Failed to bind to socket for uTP connections.", e);
		}

		connectionAcceptorThread = new Thread(connectionAcceptorRunnable, "uTP Connection Acceptor");
		connectionAcceptorThread.setDaemon(true);
		connectionAcceptorThread.start();
	}

	/**
	 * Attempts to register the given socket.
	 * @param socket The socket to register.
	 * @return <code>true</code> when the socket is registered. <code>false</code> when the connection id conflicts with an existing socket.
	 */
	public boolean registerSocket(UtpSocketImpl socket) {
		synchronized (socketListLock) {
			if (utpSockets.containsKey(socket.getReceivingConnectionId())) {
				return false;
			}

			ScheduledFuture<?> pollingTask = torrentClient.getExecutorService().scheduleAtFixedRate(() -> {
				try {
					socket.handleTimeout();
					socket.handleClose();
				} catch (IOException e) {
					LOGGER.warn("Failed to handle socket timeout/close cases. Triggering reset on socket.", e);
					resetSocket(socket);
				}
			}, 1000, 500, TimeUnit.MILLISECONDS);
			utpSockets.put(socket.getReceivingConnectionId(), new UtpSocketRegistration(socket, pollingTask));
		}
		return true;
	}

	private void resetSocket(UtpSocketImpl socket) {
		try {
			socket.onReset();
		} catch (IOException e) {
			LOGGER.warn("Failed to trigger reset on socket, state is corrupted removing socket from system.", e);
			cleanUpSocket(socket);
		}
	}

	/**
	 * Frees up the connection id used by the given UtpSocket and stops the polling for timeouts.
	 * @param socket The socket to clean up.
	 */
	public void cleanUpSocket(UtpSocketImpl socket) {
		UtpSocketRegistration registration = utpSockets.remove(socket.getReceivingConnectionId());
		registration.getPollingTask().cancel(false);
	}

	/**
	 * Sends the given datagram packet onto the socket.
	 * @param datagramPacket The packet to send.
	 * @throws IOException When the sending fails.
	 */
	public synchronized void send(DatagramPacket datagramPacket) throws IOException {
		multiplexerSocket.send(datagramPacket);
	}

	@Override
	public void run() {
		try {
			byte[] dataBuffer = new byte[25600]; //25kB buffer
			DatagramPacket packet = new DatagramPacket(dataBuffer, dataBuffer.length);
			multiplexerSocket.receive(packet);
			handlePacket(packet);
		} catch (IOException e) {
			LOGGER.warn("Failed to process uTP packet", e);
		}
	}

	private void handlePacket(DatagramPacket packet) throws IOException {
		try {
			boolean newSocket = false;
			InStream inStream = new InStream(packet.getData(), packet.getOffset(), packet.getLength());
			UtpPacket utpPacket = new UtpPacket();
			utpPacket.read(inStream, packetFactory);
			UtpSocketRegistration socketRegistration = utpSockets.get(utpPacket.getConnectionId());

			UtpSocketImpl socket;
			if (socketRegistration == null) {
				LOGGER.debug("Received connection from {}", packet.getSocketAddress());
				utpSocketFactory.setSocketAddress(packet.getSocketAddress());
				socket = utpSocketFactory.build(utpPacket.getConnectionId());
				registerSocket(socket);
				newSocket = true;
			} else {
				socket = socketRegistration.getSocket();
			}

			socket.process(utpPacket);

			if (newSocket) {
				connectionAcceptor.onReceivedConnection(new UtpSocket(this, socket));
			}
		} catch (IllegalArgumentException e) {
			LOGGER.debug("Invalid Packet of {} bytes ({}:{}).", packet.getLength(), packet.getAddress(), packet.getPort(), e);
		}
	}

	/**
	 * @return The buffer size as reported by {@link #multiplexerSocket}.
	 */
	public int getReceiveBufferSize() {
		return receiveBufferSize;
	}

	/**
	 * Closes the socket.
	 */
	public void shutdown() {
		multiplexerSocket.close();
		connectionAcceptorRunnable.stop();
		connectionAcceptorThread.interrupt();
	}
}
