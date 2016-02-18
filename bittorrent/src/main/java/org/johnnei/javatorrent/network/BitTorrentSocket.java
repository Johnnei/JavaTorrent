package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.bittorrent.protocol.MessageFactory;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.tracker.TrackerManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BitTorrentSocket {

	private static final Logger LOGGER = LoggerFactory.getLogger(BitTorrentSocket.class);

	private final Object QUEUE_LOCK = new Object();
	private final Object BLOCK_QUEUE_LOCK = new Object();

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
	 * To clock to measure the activity time with
	 */
	private Clock clock;

	/**
	 * The local timestamp at which the last activity on the underlying socket was made
	 */
	private LocalDateTime lastActivity;

	public BitTorrentSocket(Clock clock, MessageFactory messageFactory) {
		this.clock = clock;
		this.messageFactory = messageFactory;
		messageQueue = new LinkedList<>();
		blockQueue = new LinkedList<>();
		lastActivity = LocalDateTime.now(clock);
	}
	public BitTorrentSocket(MessageFactory messageFactory) {
		this(Clock.systemDefaultZone(), messageFactory);
	}

	public BitTorrentSocket(MessageFactory messageFactory, ISocket socket) throws IOException {
		this(messageFactory);
		this.socket = socket;
		createIOStreams();
	}

	public void connect(ConnectionDegradation degradation, InetSocketAddress address) throws IOException {
		if (socket != null) {
			return;
		}

		BitTorrentSocketException exception = new BitTorrentSocketException("Failed to connect to end point.");
		socket = degradation.createPreferedSocket();
		while (socket != null && (socket.isClosed() || socket.isConnecting())) {
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
	 * @param message
	 */
	public void enqueueMessage(IMessage message) {
		if (message.getId() == BitTorrent.MESSAGE_PIECE) {
			synchronized (BLOCK_QUEUE_LOCK) {
				blockQueue.add(message);
			}
		} else {
			synchronized (QUEUE_LOCK) {
				messageQueue.add(message);
			}
		}
	}

	private void createIOStreams() throws IOException {
		inStream = new ByteInputStream(socket.getInputStream());
		outStream = new ByteOutputStream(socket.getOutputStream());
	}

	public IMessage readMessage() {
		InStream stream = getBufferedMessage();
		Duration duration = getBufferLifetime();
		int length = stream.readInt();
		if (length == 0) {
			return new MessageKeepAlive();
		}

		int id = stream.readByte();
		IMessage message = messageFactory.createById(id);
		message.setReadDuration(duration);
		message.read(stream);
		return message;
	}

	/**
	 * Sends at most 1 pending message. {@link org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock} will be send last
	 * @throws IOException
	 */
	public void sendMessage() throws IOException {
		IMessage message = null;

		if (!messageQueue.isEmpty()) {
			synchronized (QUEUE_LOCK) {
				message = messageQueue.poll();
			}
		} else if (!blockQueue.isEmpty()) {
			synchronized (BLOCK_QUEUE_LOCK) {
				message = blockQueue.poll();
			}
		}

		if (message == null) {
			return;
		}

		OutStream outBuffer = new OutStream(message.getLength() + 4);
		outBuffer.writeInt(message.getLength());

		if (message.getLength() > 0) {
			outBuffer.writeByte(message.getId());
			message.write(outBuffer);
		}

		outStream.write(outBuffer.toByteArray());
		lastActivity = LocalDateTime.now(clock);
	}

	/**
	 * Writes the handshake onto the output stream
	 *
	 * @param extensionBytes The bytes indicating which extensions we support
	 * @param peerId The peer ID which has been received from {@link TrackerManager#getPeerId()}
	 * @param torrentHash The hash of the torrent on which we wish to interact on with this peer.
	 * @throws IOException
	 */
	public void sendHandshake(byte[] extensionBytes, byte[] peerId, byte[] torrentHash) throws IOException {
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
	 *             when either an io error occurs or a protocol error occurs
	 */
	public BitTorrentHandshake readHandshake() throws IOException {
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

	public void pollRates() {
		if (inStream != null) {
			downloadRate = inStream.getSpeed();
			inStream.reset(downloadRate);
		}
		if (outStream != null) {
			uploadRate = outStream.getSpeed();
			outStream.reset(uploadRate);
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

	public boolean canReadMessage() throws IOException {
		if (!passedHandshake) {
			return inStream.available() >= HANDSHAKE_SIZE;
		}

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

		return bufferSize - buffer.size() == 0;
	}

	private InStream getBufferedMessage() {
		InStream bufferedStream = new InStream(buffer.toByteArray());
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


	public int getDownloadRate() {
		return downloadRate;
	}

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
		return !messageQueue.isEmpty() || !blockQueue.isEmpty();
	}

	public String getHandshakeProgress() throws IOException {
		return String.format("%d/%d bytes", inStream.available(), HANDSHAKE_SIZE);
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

	@Override
	public String toString() {
		return socket.toString();
	}

	/**
	 * Gets the time at which the last byte has been read or written to the socket.
	 * @return The most recent activity time
	 */
	public LocalDateTime getLastActivity() {
		return lastActivity;
	}
}
