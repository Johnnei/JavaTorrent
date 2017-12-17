package org.johnnei.javatorrent.network;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.bittorrent.protocol.MessageFactory;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.internal.network.ByteInputStream;
import org.johnnei.javatorrent.internal.network.ByteOutputStream;
import org.johnnei.javatorrent.network.socket.ISocket;

public class BitTorrentSocket {

	private static final Logger LOGGER = LoggerFactory.getLogger(BitTorrentSocket.class);

	private final Object queueLock = new Object();
	private final Object blockQueueLock = new Object();

	/**
	 * Clock instance to allow for speedy unit tests on this.
	 */
	private Clock clock = Clock.systemDefaultZone();

	private static final int HANDSHAKE_SIZE = 68;

	private ISocket socket;

	private ByteInputStream inStream;

	private ByteOutputStream outStream;

	private final MessageFactory messageFactory;

	/**
	 * The amount of bytes read in the last second
	 */
	private int downloadRate;

	/**
	 * The amount of bytes written in the last second
	 */
	private int uploadRate;

	/**
	 * Remembers if this socket has read the handshake information or not
	 */
	private boolean passedHandshake;

	/**
	 * The queue containing the messages which still have to be send
	 */
	private Queue<IMessage> messageQueue;

	/**
	 * The queue containing the block message which still have to be send
	 */
	private Queue<IMessage> blockQueue;

	private OutStream buffer;

	private int bufferSize;

	/**
	 * The last time a buffer was created
	 */
	private LocalDateTime lastBufferCreate;

	/**
	 * The local timestamp at which the last activity on the underlying socket was made
	 */
	private LocalDateTime lastActivity;

	/**
	 * Creates a new unbound BitTorrent socket.
	 * @param messageFactory The factory to create {@link IMessage} instances.
	 */
	public BitTorrentSocket(MessageFactory messageFactory) {
		this.messageFactory = messageFactory;
		messageQueue = new LinkedList<>();
		blockQueue = new LinkedList<>();
		lastActivity = LocalDateTime.now(clock);
	}

	/**
	 * Creates a new bound BitTorrent socket.
	 * @param messageFactory The factory to create {@link IMessage} instances.
	 * @param socket The bound socket.
	 * @throws IOException When the IO streams can not be wrapped.
	 */
	public BitTorrentSocket(MessageFactory messageFactory, ISocket socket) throws IOException {
		this(messageFactory);
		this.socket = Objects.requireNonNull(socket, "Socket cannot be null, use other constructor instead.");
		createIOStreams();
	}

	/**
	 * Attempts to connect to the given endpoint in a blocking manner.
	 * @param degradation The socket degradation order.
	 * @param address The address to connect to
	 * @throws IOException When an IO error occur during the establishing of a connection.
	 */
	public void connect(ConnectionDegradation degradation, InetSocketAddress address) throws IOException {
		if (socket != null) {
			return;
		}

		BitTorrentSocketException exception = new BitTorrentSocketException("Failed to connect to end point.");
		socket = degradation.createPreferredSocket();
		while (socket.isClosed()) {
			try {
				socket.connect(address);
				createIOStreams();
			} catch (IOException e) {
				exception.addConnectionFailure(socket, e);
				Optional<ISocket> fallbackSocket = degradation.degradeSocket(socket);
				if (fallbackSocket.isPresent()) {
					socket = fallbackSocket.get();
				} else {
					throw exception;
				}
			}
		}
	}

	/**
	 * Queues the message to be send
	 * @param message The message to be added to the queue
	 */
	public void enqueueMessage(IMessage message) {
		if (message instanceof MessageBlock) {
			synchronized (blockQueueLock) {
				blockQueue.add(message);
			}
		} else {
			synchronized (queueLock) {
				messageQueue.add(message);
			}
		}
	}

	private void createIOStreams() throws IOException {
		inStream = new ByteInputStream(new BufferedInputStream(socket.getInputStream()));
		outStream = new ByteOutputStream(new BufferedOutputStream(socket.getOutputStream()));
	}

	public IMessage readMessage() {

		InStream stream = getBufferedMessage();
		int length = stream.readInt();
		if (length == 0) {
			return new MessageKeepAlive();
		}

		int id = stream.readByte();
		IMessage message = messageFactory.createById(id);
		message.read(stream);

		LOGGER.trace("Read message: {}", message);
		return message;
	}

	/**
	 * Sends at most 1 pending message. {@link org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock} will be send last
	 * @throws IOException
	 */
	public void sendMessage() throws IOException {
		IMessage message = null;

		if (!messageQueue.isEmpty()) {
			synchronized (queueLock) {
				message = messageQueue.poll();
			}
		} else if (!blockQueue.isEmpty()) {
			synchronized (blockQueueLock) {
				message = blockQueue.poll();
			}
		}

		if (message == null) {
			return;
		}

		LOGGER.trace("Writing message {}", message);

		OutStream outBuffer = new OutStream(message.getLength() + 4);
		outBuffer.writeInt(message.getLength());

		if (message.getLength() > 0) {
			outBuffer.writeByte(message.getId());
			message.write(outBuffer);
		}

		outStream.write(outBuffer.toByteArray());
		outStream.flush();
		lastActivity = LocalDateTime.now(clock);
	}

	/**
	 * Writes the handshake onto the output stream
	 *
	 * @param extensionBytes The bytes indicating which extensions we support
	 * @param peerId The peer ID which this peer will send out.
	 * @param torrentHash The hash of the torrent on which we wish to interact on with this peer.
	 * @throws IOException
	 * @Deprecated
	 */
	@Deprecated
	public void sendHandshake(byte[] extensionBytes, byte[] peerId, byte[] torrentHash) throws IOException {
		if (passedHandshake) {
			throw new IllegalStateException("Handshake has already been completed.");
		}

		LOGGER.debug("Writing handshake", socket);

		outStream.writeByte(0x13);
		outStream.writeString("BitTorrent protocol");
		outStream.write(extensionBytes);
		outStream.write(torrentHash);
		outStream.write(peerId);
		outStream.flush();
	}

	/**
	 * Reads the handshake information from the peer
	 *
	 * @return A successfully read handshake
	 * @throws IOException
	 *             when either an io error occurs, a protocol error occurs or the peer doesn't respond within 5 seconds.
	 */
	public BitTorrentHandshake readHandshake() throws IOException {
		if (passedHandshake) {
			throw new IllegalStateException("Handshake has already been completed.");
		}

		awaitHandshake();

		int protocolLength = inStream.read();
		if (protocolLength != 0x13) {
			throw new IOException("Protocol handshake failed");
		}

		String protocol = inStream.readString(0x13);

		if (!"BitTorrent protocol".equals(protocol)) {
			throw new IOException("Protocol handshake failed");
		}

		byte[] extensionBytes = inStream.readByteArray(8);
		byte[] torrentHash = inStream.readByteArray(20);
		byte[] peerId = inStream.readByteArray(20);

		return new BitTorrentHandshake(torrentHash, extensionBytes, peerId);
	}

	private void awaitHandshake() throws IOException {
		LocalDateTime startTime = LocalDateTime.now(clock);

		while (Duration.between(startTime, LocalDateTime.now(clock)).minusSeconds(5).isNegative() && inStream.available() < HANDSHAKE_SIZE) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				// Honour the cancellation and rethrow it as IOException
				throw new IOException(e);
			}
		}

		if (inStream.available() < HANDSHAKE_SIZE) {
			LOGGER.debug("Remote end supplied {} out of {} bytes.", inStream.available(), HANDSHAKE_SIZE);
			throw new IOException("Remote end failed to supply handshake within 5 seconds");
		}

	}

	/**
	 * Polls all the transfer speeds.
	 */
	public void pollRates() {
		if (inStream != null) {
			downloadRate = inStream.pollSpeed();
		}
		if (outStream != null) {
			uploadRate = outStream.pollSpeed();
		}
	}

	/**
	 * Closes the connection with the socket
	 */
	public void close() {
		if (socket.isClosed()) {
			return;
		}

		try {
			socket.close();
		} catch (IOException e) {
			LOGGER.warn("Failed to close socket.", e);
		}
	}

	/**
	 * Buffers the next message for reading.
	 * @return Returns <code>true</code> when enough data is buffered to read the next message without blocking.
	 * @throws IOException When an IO error occurs during the buffering.
	 */
	public boolean canReadMessage() throws IOException {
		if (buffer == null) {
			if (inStream.available() < 4) {
				return false;
			}

			lastBufferCreate = LocalDateTime.now(clock);
			int length = inStream.readInt();
			buffer = new OutStream(length + 4);
			bufferSize = length + 4;
			buffer.writeInt(length);
		}

		int remainingBytes = bufferSize - buffer.size();
		if (remainingBytes == 0) {
			return true;
		}

		int availableBytes = Math.min(remainingBytes, inStream.available());
		buffer.write(inStream.readByteArray(availableBytes));
		lastActivity = LocalDateTime.now(clock);

		return bufferSize - buffer.size() == 0;
	}

	private InStream getBufferedMessage() {
		InStream bufferedStream = new InStream(buffer.toByteArray(), getBufferLifetime());
		buffer = null;
		return bufferedStream;
	}

	/**
	 * The time that this buffer has existed
	 *
	 * @return The duration since creation
	 */
	private Duration getBufferLifetime() {
		return Duration.between(lastBufferCreate, LocalDateTime.now(clock));
	}


	/**
	 * Returns the last polled download rate.
	 * @return The amount of downloaded bytes
	 *
	 * @see #pollRates()
	 */
	public int getDownloadRate() {
		return downloadRate;
	}

	/**
	 * Returns the last polled upload rate.
	 * @return The amount of uploaded bytes
	 *
	 * @see #pollRates()
	 */
	public int getUploadRate() {
		return uploadRate;
	}

	/**
	 * Checks if the socket is closed
	 * @return <code>true</code> if the underlying socket is closed, otherwise <code>false</code>
	 */
	public boolean closed() {
		if (socket == null) {
			return true;
		}

		return socket.isClosed();
	}

	/**
	 * Gets if this socket has completed the BitTorrent handshake
	 * @return <code>true</code> if the handshake was completed
	 */
	public boolean getPassedHandshake() {
		return passedHandshake;
	}

	/**
	 * Marks that this socket has passed the BitTorrent handshake and therefor is a valid BitTorrent socket.
	 */
	public void setPassedHandshake() {
		passedHandshake = true;
	}

	/**
	 * Checks if this socket has messages queued for sending
	 * @return <code>true</code> if there is at least one {@link IMessage} waiting to be sent.
	 */
	public boolean hasOutboundMessages() {
		LOGGER.trace("Pending outbound messages [{}] blocks [{}]", messageQueue.size(), blockQueue.size());
		return !messageQueue.isEmpty() || !blockQueue.isEmpty();
	}

	/**
	 * Gets the class simple name of the underlying socket.
	 * @return The name of the socket or an empty string when no socket is set.
	 */
	public String getSocketName() {
		if (socket == null) {
			return "";
		}

		return socket.getClass().getSimpleName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return String.format("BitTorrentSocket[socket=%s]", socket);
	}

	/**
	 * Gets the time at which the last byte has been read or written to the socket.
	 * @return The most recent activity time
	 */
	public LocalDateTime getLastActivity() {
		return lastActivity;
	}
}
