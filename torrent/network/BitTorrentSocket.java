package torrent.network;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Queue;

import torrent.JavaTorrent;
import torrent.download.tracker.TrackerManager;
import torrent.network.protocol.ISocket;
import torrent.network.protocol.TcpSocket;
import torrent.protocol.BitTorrent;
import torrent.protocol.BitTorrentHandshake;
import torrent.protocol.IMessage;
import torrent.protocol.MessageUtils;

public class BitTorrentSocket {
	
	private final Object QUEUE_LOCK = new Object();
	private final Object BLOCK_QUEUE_LOCK = new Object();
	
	private static final int HANDSHAKE_SIZE = 68;
	
	private ISocket socket;
	
	private ByteInputStream inStream;
	
	private ByteOutputStream outStream;
	
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
	
	public BitTorrentSocket() {
		messageQueue = new LinkedList<>();
		blockQueue = new LinkedList<>();
	}
	
	public BitTorrentSocket(ISocket socket) throws IOException {
		this();
		this.socket = socket;
		createIOStreams();
	}
	
	public void connect(InetSocketAddress address) throws IOException {
		if (socket != null) {
			return;
		}
		
		socket = new TcpSocket();
		while (socket != null && (socket.isClosed() || socket.isConnecting())) {
			try {
				socket.connect(address);
				createIOStreams();
			} catch (IOException e) {
				if (socket.canFallback()) {
					socket = socket.getFallbackSocket();
				} else {
					throw new IOException(String.format("Failed to connect to end point with all socket types"));
				}
			}
		}
	}

	/**
	 * Queues the message to be send
	 * @param message
	 */
	public void queueMessage(IMessage message) {
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
	
	public IMessage readMessage() throws IOException {
		return MessageUtils.getUtils().readMessage(inStream);
	}
	
	/**
	 * Sends at most 1 pending message. {@link MessagePiece} will be send last
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
		
		MessageUtils.getUtils().writeMessage(outStream, message);
	}
	
	/**
	 * Writes the handshake onto the output stream
	 * 
	 * @param peerId
	 *            The peer ID which has been received from
	 *            {@link TrackerManager#getPeerId()}
	 * @throws IOException
	 */
	public void sendHandshake(byte[] peerId, byte[] torrentHash) throws IOException {
		outStream.writeByte(0x13);
		outStream.writeString("BitTorrent protocol");
		outStream.write(JavaTorrent.RESERVED_EXTENTION_BYTES);
		outStream.write(torrentHash);
		outStream.write(peerId);
		outStream.flush();
	}
	
	/**
	 * Reads the handshake information from the peer
	 * 
	 * @return A succesfully read handshake
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
		}
	}
	
	public boolean canReadMessage() throws IOException {
		if (!passedHandshake) {
			return inStream.available() >= HANDSHAKE_SIZE;
		}
		return MessageUtils.getUtils().canReadMessage(inStream);
	}

	public int getDownloadRate() {
		return downloadRate;
	}

	public int getUploadRate() {
		return uploadRate;
	}
	
	/**
	 * Checks if the socket is closed
	 * @return
	 */
	public boolean closed() {
		if (socket == null) {
			return true;
		}
		
		return socket.isClosed();
	}
	
	public boolean getPassedHandshake() {
		return passedHandshake;
	}

}
