package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.bittorrent.protocol.MessageFactory;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.internal.network.TransferRate;
import org.johnnei.javatorrent.network.socket.ISocket;

import static org.johnnei.javatorrent.network.ByteBufferUtils.getBytes;

/**
 * Handles the raw data on the {@link ByteChannel} provided by {@link ISocket}.
 */
public class BitTorrentSocket {

	private static final Logger LOGGER = LoggerFactory.getLogger(BitTorrentSocket.class);

	private static final int MESSAGE_LENGTH_SIZE = 4;

	/**
	 * The amount of bytes reserved for {@link #readBuffer}.
	 * The size is chosen to be the 'largest' regular packet. ({@link MessageBlock}
	 */
	private static final int READ_BUFFER_SIZE = (1 << 14) + MESSAGE_LENGTH_SIZE;

	private final Object queueLock = new Object();
	private final Object blockQueueLock = new Object();

	/**
	 * Clock instance to allow for speedy unit tests on this.
	 */
	private final Clock clock;

	private final ISocket socket;

	private final MessageFactory messageFactory;

	private final TransferRate downloadRate;

	private final TransferRate uploadRate;

	/**
	 * The queue containing the messages which still have to be send
	 */
	private Queue<IMessage> messageQueue;

	/**
	 * The queue containing the block message which still have to be send
	 */
	private Queue<IMessage> blockQueue;

	/**
	 * Buffer holding incomplete {@link IMessage} binary data.
	 */
	private ByteBuffer readBuffer;

	/**
	 * Buffer holding incomplete sent {@link IMessage} binary data.
	 */
	private ByteBuffer writeBuffer;

	/**
	 * The last time a buffer was created
	 */
	private LocalDateTime lastBufferCreate;

	/**
	 * The local timestamp at which the last activity on the underlying socket was made
	 */
	private LocalDateTime lastActivity;

	/**
	 * Creates a new bound BitTorrent socket.
	 * @param messageFactory The factory to create {@link IMessage} instances.
	 * @param socket The bound socket.
	 */
	public BitTorrentSocket(MessageFactory messageFactory, ISocket socket) {
		this(messageFactory, socket, Clock.systemDefaultZone());
	}

	BitTorrentSocket(MessageFactory messageFactory, ISocket socket, Clock clock) {
		this.clock = clock;
		this.messageFactory = messageFactory;
		messageQueue = new LinkedList<>();
		blockQueue = new LinkedList<>();
		lastActivity = LocalDateTime.now(clock);
		this.socket = Objects.requireNonNull(socket, "Socket cannot be null");
		this.readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
		this.readBuffer.limit(0);
		this.downloadRate = new TransferRate(clock);
		this.uploadRate = new TransferRate(clock);
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

	/**
	 * Converts the buffered message to an {@link IMessage}. <em>must</em> only be called when {@link #canReadMessage()} returns <code>true</code>.
	 * @return The next message on the stream.
	 */
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
	 * Sends messages until the operation is no longer blocking. {@link org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock} will be send last.
	 * @throws IOException When write fails.
	 */
	public void sendMessages() throws IOException {
		ByteBuffer buffer;

		while ((buffer = prepareMessageForSending()) != null) {
			int transferredBytes = ((WritableByteChannel) socket.getWritableChannel()).write(buffer);

			uploadRate.addTransferredBytes(transferredBytes);
			lastActivity = LocalDateTime.now(clock);

			if (buffer.hasRemaining()) {
				writeBuffer = buffer;
				return;
			} else {
				writeBuffer = null;
			}

		}
	}

	private ByteBuffer prepareMessageForSending() {
		if (writeBuffer != null) {
			return writeBuffer;
		}

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
			return null;
		}

		LOGGER.trace("Writing message {}", message);

		OutStream outBuffer = new OutStream(message.getLength() + 4);
		outBuffer.writeInt(message.getLength());

		if (message.getLength() > 0) {
			outBuffer.writeByte(message.getId());
			message.write(outBuffer);
		}

		return ByteBuffer.wrap(outBuffer.toByteArray());
	}

	/**
	 * Polls all the transfer speeds.
	 */
	public void pollRates() {
		downloadRate.pollRate();
		uploadRate.pollRate();
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
		if (readBuffer.position() == 0) {
			lastBufferCreate = LocalDateTime.now(clock);
			readBuffer.limit(4);
		}

		readInput();

		if (readBuffer.position() >= MESSAGE_LENGTH_SIZE) {
			ByteBuffer buffer = readBuffer.asReadOnlyBuffer();
			buffer.flip();
			int messageLength = buffer.getInt();
			int bytesNeeded = MESSAGE_LENGTH_SIZE + messageLength;

			if (readBuffer.capacity() < bytesNeeded) {
				growReadBuffer(bytesNeeded);
			}

			readBuffer.limit(bytesNeeded);
			if (readBuffer.hasRemaining()) {
				readInput();
			}

			return readBuffer.position() >= bytesNeeded;
		}

		return false;
	}

	private void readInput() throws IOException {
		int readBytes = ((ReadableByteChannel) socket.getReadableChannel()).read(readBuffer);
		if (readBytes == -1) {
			throw new IOException("Unexpected end of channel.");
		} else {
			downloadRate.addTransferredBytes(readBytes);
		}
	}

	private void growReadBuffer(int desiredSize) {
		ByteBuffer buffer = ByteBuffer.allocate(desiredSize);
		readBuffer.flip();
		buffer.put(readBuffer);
		this.readBuffer = buffer;
	}

	private InStream getBufferedMessage() {
		readBuffer.flip();
		InStream messageStream = new InStream(getBytes(readBuffer, readBuffer.remaining()), getBufferLifetime());
		readBuffer.clear();
		return messageStream;
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
		return downloadRate.getRate();
	}

	/**
	 * Returns the last polled upload rate.
	 * @return The amount of uploaded bytes
	 *
	 * @see #pollRates()
	 */
	public int getUploadRate() {
		return uploadRate.getRate();
	}

	/**
	 * Checks if the socket is closed
	 * @return <code>true</code> if the underlying socket is closed, otherwise <code>false</code>
	 */
	public boolean closed() {
		return socket.isClosed();
	}

	/**
	 * Checks if this socket has messages queued for sending
	 * @return <code>true</code> if there is at least one {@link IMessage} waiting to be sent.
	 */
	public boolean hasOutboundMessages() {
		boolean hasPendingMessages = !messageQueue.isEmpty() || !blockQueue.isEmpty();
		if (hasPendingMessages) {
			LOGGER.trace("Pending outbound messages [{}] blocks [{}]", messageQueue.size(), blockQueue.size());
		}
		return writeBuffer != null || hasPendingMessages;
	}

	/**
	 * Gets the class simple name of the underlying socket.
	 * @return The name of the socket or an empty string when no socket is set.
	 */
	public String getSocketName() {
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
