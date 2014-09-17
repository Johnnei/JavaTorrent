package torrent.network;

import java.io.IOException;
import java.net.InetSocketAddress;

import torrent.JavaTorrent;
import torrent.download.tracker.TrackerManager;
import torrent.network.protocol.ISocket;
import torrent.network.protocol.TcpSocket;
import torrent.protocol.BitTorrentHandshake;
import torrent.protocol.IMessage;
import torrent.protocol.MessageUtils;

public class BitTorrentSocket {
	
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
	
	public BitTorrentSocket() {
	}
	
	public BitTorrentSocket(ISocket socket) throws IOException {
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
	
	private void createIOStreams() throws IOException {
		inStream = new ByteInputStream(socket.getInputStream());
		outStream = new ByteOutputStream(socket.getOutputStream());
	}
	
	public IMessage readMessage() throws IOException {
		return MessageUtils.getUtils().readMessage(inStream);
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

}
