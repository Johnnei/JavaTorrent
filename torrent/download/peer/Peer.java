package torrent.download.peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.johnnei.utils.ConsoleLogger;

import torrent.download.Torrent;
import torrent.download.files.disk.DiskJobSendBlock;
import torrent.download.tracker.TrackerManager;
import torrent.network.ByteInputStream;
import torrent.network.ByteOutputStream;
import torrent.network.protocol.ISocket;
import torrent.network.protocol.TcpSocket;
import torrent.protocol.BitTorrentHandshake;
import torrent.protocol.IMessage;
import torrent.protocol.MessageUtils;
import torrent.protocol.messages.MessageKeepAlive;

public class Peer implements Comparable<Peer> {

	private byte[] RESERVED_EXTENTION_BYTES = new byte[8];
	private static final int HANDSHAKE_SIZE = 68;

	private InetAddress address;
	private int port;

	private Torrent torrent;
	private ISocket socket;
	private ByteOutputStream outStream;
	private ByteInputStream inStream;
	private boolean crashed;
	/**
	 * Client information about the connected peer
	 */
	private Client peerClient;
	/**
	 * Client information about me retrieved from the connected peer
	 */
	private Client myClient;
	/**
	 * The current Threads status
	 */
	private String status;
	/**
	 * The message queue
	 */
	private ArrayList<IMessage> messageQueue;
	/**
	 * The last time this connection showed any form of activity<br/>
	 * <i>Values are System.currentMillis()</i>
	 */
	private long lastActivity;

	private int downloadRate;
	private int uploadRate;
	private boolean passedHandshake;

	private String clientName;
	/**
	 * The count of messages which are still being processed by the IOManager
	 */
	private int pendingMessages;
	/**
	 * The amount of errors the client made<br/>
	 * If the amount reaches 5 the client will be disconnected on the next
	 * peerCheck
	 */
	private int strikes;
	
	private Logger log;

	public Peer() {
		crashed = false;
		peerClient = new Client();
		myClient = new Client();
		status = "";
		clientName = "pending";
		messageQueue = new ArrayList<>();
		RESERVED_EXTENTION_BYTES[5] |= 0x10; // Extended Messages
		lastActivity = System.currentTimeMillis();
		passedHandshake = false;
		log = ConsoleLogger.createLogger("Peer", Level.INFO);
	}

	public Peer(Torrent torrent) {
		this();
		this.torrent = torrent;
	}

	public void connect() {
		setStatus("Connecting...");
		if (socket != null) {
			setStatus("Connected (Outside request)");
			log = ConsoleLogger.createLogger(String.format("Peer %s", socket.toString()), Level.INFO);
			return;
		}
		socket = new TcpSocket();
		while (socket != null && (socket.isClosed() || socket.isConnecting())) {
			try {
				socket.connect(new InetSocketAddress(address, port));
				inStream = new ByteInputStream(this, socket.getInputStream());
				outStream = new ByteOutputStream(socket.getOutputStream());
				log = ConsoleLogger.createLogger(String.format("Peer %s", socket.toString()), Level.INFO);
			} catch (IOException e) {
				if (socket.canFallback()) {
					socket = socket.getFallbackSocket();
				} else {
					crashed = true;
					socket = null;
				}
			}
		}
	}

	public void setSocket(ISocket socket) {
		this.socket = socket;
		try {
			inStream = new ByteInputStream(this, socket.getInputStream());
			outStream = new ByteOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			log.warning(e.getMessage());
		}
	}

	public void setSocketInformation(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}

	/**
	 * Writes the handshake onto the output stream
	 * 
	 * @param peerId
	 *            The peer ID which has been received from
	 *            {@link TrackerManager#getPeerId()}
	 * @throws IOException
	 */
	public void sendHandshake(byte[] peerId) throws IOException {
		setStatus("Sending Handshake"); // Not making this more OO because it's
										// not within the message-flow
		outStream.writeByte(0x13);
		outStream.writeString("BitTorrent protocol");
		outStream.write(RESERVED_EXTENTION_BYTES);
		outStream.write(torrent.getHashArray());
		outStream.write(peerId);
		outStream.flush();
		setStatus("Awaiting Handshake response");
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

	public boolean canReadMessage() throws IOException {
		if (!passedHandshake) {
			return inStream.available() >= HANDSHAKE_SIZE;
		}
		return MessageUtils.getUtils().canReadMessage(inStream, this);
	}

	public void readMessage() throws IOException {
		IMessage message = MessageUtils.getUtils().readMessage(inStream, this);
		message.process(this);
		lastActivity = System.currentTimeMillis();
	}

	public void sendMessage() throws IOException {
		if (messageQueue.size() > 0) {
			IMessage message = messageQueue.remove(0);
			if (message == null)
				return;
			setStatus("Sending Message: " + message);
			MessageUtils.getUtils().writeMessage(outStream, message);
			setStatus("Sended Message: " + message);
			if (messageQueue.size() == 0) {// Last message has been sended
				// We dont expect new messages to be send shortly anymore, flush
				// the holded data
				outStream.flush();
			}
		} else {
			if (peerClient.getQueueSize() > 0 && pendingMessages == 0) {
				Job request = peerClient.getNextJob();
				peerClient.removeJob(request);
				addToPendingMessages(1);
				torrent.addDiskJob(new DiskJobSendBlock(this, request
						.getPieceIndex(), request.getBlockIndex(), request
						.getLength()));
			}
		}
	}

	public void checkDisconnect() {
		if (strikes >= 5) {
			close();
			return;
		}
		int inactiveSeconds = (int) ((System.currentTimeMillis() - lastActivity) / 1000);
		if (inactiveSeconds > 30) {
			if (myClient.getQueueSize() > 0) { // We are not receiving a single
												// byte in the last 30(!)
												// seconds
				// Let's try (max twice) if we can wake'm up by sending them a
				// keepalive
				addStrike(2);
				addToQueue(new MessageKeepAlive());
				updateLastActivity();
				return;
			} else if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA) {
				updateLastActivity();
			}
		}
		if (inactiveSeconds > 60) {
			if (myClient.isInterested() && myClient.isChoked()) {
				addStrike(2);
				updateLastActivity();
			}
		}

		if (inactiveSeconds > 90) {// 1.5 Minute, We are getting close to
									// timeout D:
			if (myClient.isInterested()) {
				addToQueue(new MessageKeepAlive());
			}
		}
		if (inactiveSeconds > 180) {// 3 Minutes, We've hit the timeout mark
			close();
		}
	}

	/**
	 * Add a value to the pending Messages count
	 * 
	 * @param i
	 *            The count to add
	 */
	public synchronized void addToPendingMessages(int i) {
		pendingMessages += i;
	}

	/**
	 * Checks if the connection is/should be closed
	 * 
	 * @return If the connection should be/is closed
	 */
	public boolean closed() {
		if (crashed)
			return true;
		if (socket == null)
			return false;
		return socket.isClosed();
	}

	@Override
	public String toString() {
		if (socket != null) {
			return socket.toString();
		} else {
			return "UNCONNECTED";
		}
	}

	public void setStatus(String s) {
		status = s;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getClientName() {
		return clientName;
	}

	public String getStatus() {
		return status;
	}

	public void updateLastActivity() {
		lastActivity = System.currentTimeMillis();
	}

	/**
	 * Gets the amount of messages waiting to be send
	 * 
	 * @return
	 */
	public boolean isWorking() {
		return getWorkQueueSize() > 0;
	}

	public int getFreeWorkTime() {
		return (getWorkQueueSize() >= myClient.getMaxRequests()) ? 0 : myClient
				.getMaxRequests() - getWorkQueueSize();
	}

	public int getWorkQueueSize() {
		return myClient.getQueueSize();
	}

	public int getMaxWorkLoad() {
		return myClient.getMaxRequests();
	}

	public void addToQueue(IMessage m) {
		messageQueue.add(m);
	}

	public Client getClient() {
		return peerClient;
	}

	public Client getMyClient() {
		return myClient;
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

	public int getDownloadRate() {
		return downloadRate;
	}

	public int getUploadRate() {
		return uploadRate;
	}

	/**
	 * Cancels all pieces
	 */
	public void cancelAllPieces() {
		synchronized (this) {
			if (getWorkQueueSize() > 0) {
				Object[] keys = myClient.getKeySet().toArray();
				for (int i = 0; i < keys.length; i++) {
					Job job = (Job) keys[i];
					torrent.getFiles().getPiece(job.getPieceIndex())
							.reset(job.getBlockIndex());
				}
				myClient.clearJobs();
			}
		}
	}

	public void forceClose() {
		crashed = true;
	}

	/**
	 * Gracefully close the connection with this peer
	 */
	public void close() {
		try {
			if (socket != null) {
				if (!socket.isClosed()) {
					socket.close();
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			forceClose();
		}
	}

	/**
	 * Sets the torrent
	 * 
	 * @param torrent
	 * @throws IllegalStateException
	 *             if the peer is already bound to a torrent
	 */
	public void setTorrent(Torrent torrent) throws IllegalStateException {
		if (this.torrent != null) {
			throw new IllegalStateException(
					"Peer is already bound to a torrent");
		}

		this.torrent = torrent;
	}

	public boolean getPassedHandshake() {
		return passedHandshake;
	}

	public long getLastActivity() {
		return lastActivity;
	}

	public Torrent getTorrent() {
		return torrent;
	}

	@Override
	public int compareTo(Peer p) {
		int myValue = getCompareValue();
		int theirValue = p.getCompareValue();
		
		return myValue - theirValue;
	}
	
	private int getCompareValue() {
		return (getWorkQueueSize() * 5000) + peerClient.getBitfield().hasPieceCount() + downloadRate;
	}

	/**
	 * Adds an amount of strikes to the peer
	 * 
	 * @param i
	 */
	public synchronized void addStrike(int i) {
		if (strikes + i < 0) {
			strikes = 0;
		} else {
			strikes += i;
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Peer) {
			Peer p = (Peer) o;
			return (p.toString().equals(toString()));
		} else {
			return false;
		}
	}

	/**
	 * Adds flags to a string based on the state of a peer<br/>
	 * Possible Flags:<br/>
	 * U - Uses uTP T - Uses TCP I - Is Interested C - Is Choked
	 * 
	 * @return
	 */
	public String getFlags() {
		String flags = socket.getClass().getSimpleName().substring(0, 1);
		if (peerClient.isInterested()) {
			flags += "I";
		}
		if (peerClient.isChoked()) {
			flags += "C";
		}
		return flags;
	}
	
	public Logger getLogger() {
		return log;
	}
}
