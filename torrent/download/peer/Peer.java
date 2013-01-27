package torrent.download.peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

import org.johnnei.utils.ThreadUtils;

import torrent.Logable;
import torrent.Manager;
import torrent.download.Torrent;
import torrent.download.files.disk.DiskJobSendBlock;
import torrent.network.ByteInputStream;
import torrent.network.ByteOutputStream;
import torrent.network.UtpSocket;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;
import torrent.protocol.MessageUtils;
import torrent.protocol.messages.MessageKeepAlive;
import torrent.protocol.messages.extension.MessageExtension;
import torrent.protocol.messages.extension.MessageHandshake;
import torrent.util.ISortable;
import torrent.util.StringUtil;

public class Peer implements Logable, ISortable {

	private byte[] RESERVED_EXTENTION_BYTES = new byte[8];

	private InetAddress address;
	private int port;

	private Torrent torrent;
	private UtpSocket socket;
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
	 * If the amount reaches 5 the client will be disconnected on the next peerCheck
	 */
	private int strikes;

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
	}

	public Peer(Torrent torrent) {
		this();
		this.torrent = torrent;
	}

	public void connect() {
		try {
			setStatus("Connecting...");
			if (socket != null) {
				setStatus("Connected (Outside request)");
				return;
			}
			socket = new UtpSocket();
			socket.connect(new InetSocketAddress(address, port), 1000);
			inStream = socket.getInputStream(this);
			outStream = socket.getOutputStream();
		} catch (IOException e) {
			crashed = true;
			return;
		}
	}

	/**
	 * Send all extension handshakes
	 */
	private void sendExtensionMessage() {
		if (peerClient.supportsExtention(5, 0x10)) { // EXTENDED_MESSAGE
			if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA)
				addToQueue(new MessageExtension(BitTorrent.EXTENDED_MESSAGE_HANDSHAKE, new MessageHandshake()));
			else
				addToQueue(new MessageExtension(BitTorrent.EXTENDED_MESSAGE_HANDSHAKE, new MessageHandshake(torrent.getFiles().getMetadataSize())));
		}
	}

	/**
	 * Send have messages or bitfield
	 */
	private void sendHaveMessages() {
		if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_DATA) {
			ArrayList<IMessage> messages = torrent.getFiles().getBitfield().getBitfieldMessage();
			for (int i = 0; i < messages.size(); i++) {
				addToQueue(messages.get(i));
			}
		}
	}

	public void setSocket(UtpSocket socket) {
		this.socket = socket;
		setSocket(socket.getInetAddress(), socket.getPort());
		try {
			inStream = socket.getInputStream(this);
			outStream = socket.getOutputStream();
		} catch (IOException e) {
			log(e.getMessage(), true);
		}
	}

	public void setSocket(InetAddress address, int port) {
		this.address = address;
		this.port = port;
	}

	public void sendHandshake() throws IOException {
		setStatus("Sending Handshake"); // Not making this more OO because it's not within the message-flow
		outStream.writeByte(0x13);
		outStream.writeString("BitTorrent protocol");
		outStream.write(RESERVED_EXTENTION_BYTES);
		outStream.write(torrent.getHashArray());
		outStream.write(Manager.getPeerId());
		setStatus("Awaiting Handshake response");
	}

	public void processHandshake() throws IOException {
		status = "Verifying handshake";
		int protocolLength = inStream.read();
		if (protocolLength != 0x13) {
			socket.close();
			crashed = true;
			return;
		} else {
			String protocol = inStream.readString(0x13);
			if ("BitTorrent protocol".equals(protocol)) {
				peerClient.setReservedBytes(inStream.readByteArray(8));
				byte[] torrentHash = inStream.readByteArray(20);
				if (torrent == null) {
					Torrent torrent = Manager.getManager().getTorrent(StringUtil.byteArrayToString(torrentHash));
					if (torrent == null) {
						return;
					}
					this.torrent = torrent;
				}
				if (torrentHash != torrent.getHashArray()) {
					inStream.readByteArray(20);
					if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_DATA)
						peerClient.getBitfield().setBitfieldSize(torrent.getFiles().getBitfieldSize());
					setStatus("Awaiting Orders");
					passedHandshake = true;
				} else {
					socket.close();
					crashed = true;
					return;
				}
			} else {
				crashed = true;
				socket.close();
				return;
			}
		}
		sendExtensionMessage();
		sendHaveMessages();
	}

	public void run() {
		while (torrent.keepDownloading() && !closed()) {
			try {
				if (!passedHandshake)
					processHandshake();
				readMessage();
				sendMessage();
			} catch (IOException e) {
				close();
			}
			ThreadUtils.sleep(1);
		}
		close();
		setStatus("Connection closed");
	}

	public boolean canReadMessage() throws IOException {
		if (!passedHandshake) {
			return inStream.available() >= 68;
		}
		return MessageUtils.getUtils().canReadMessage(inStream, this);
	}

	public void readMessage() throws IOException {
		if (!passedHandshake) {
			processHandshake();
			return;
		}
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
		} else {
			if (peerClient.getQueueSize() > 0 && pendingMessages == 0) {
				Job request = peerClient.getNextJob();
				peerClient.removeJob(request);
				addToPendingMessages(1);
				torrent.addDiskJob(new DiskJobSendBlock(this, request.getPieceIndex(), request.getBlockIndex(), request.getLength()));
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
			if (myClient.getQueueSize() > 0) { // We are not receiving a single byte in the last 30(!) seconds
				// Let's try (max twice) if we can wake'm up by sending them a keepalive
				addStrike(2);
				addToQueue(new MessageKeepAlive());
				updateLastActivity();
				return;
			} else if (torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_METADATA) {
				updateLastActivity();
			}
		}
		if (inactiveSeconds > 60) {
			if(myClient.isInterested() && myClient.isChoked()) {
				addStrike(2);
				updateLastActivity();
			}
		}
		if (inactiveSeconds > 90) {// 1.5 Minute, We are getting close to timeout D:
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
	 * @param i The count to add
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
		if (socket != null)
			return address.getHostAddress() + ":" + port;
		if (address != null && port > 0)
			return address.getHostAddress() + ":" + port;
		return "UNCONNECTED";
	}

	@Override
	public void log(String s, boolean error) {
		s = "[" + toString() + "] " + s;
		if (error)
			System.err.println(s);
		else
			System.out.println(s);
	}

	@Override
	public void log(String s) {
		log(s, false);
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

	@Override
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
		return (getWorkQueueSize() >= myClient.getMaxRequests()) ? 0 : myClient.getMaxRequests() - getWorkQueueSize();
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
					torrent.getFiles().getPiece(job.getPieceIndex()).reset(job.getBlockIndex());
				}
				myClient.clearJobs();
			}
		}
	}

	/**
	 * Gracefully close the connection with this peer
	 */
	public void close() {
		try {
			if (socket != null) {
				if (!socket.isClosed()) {
					socket.shutdownInput();
					socket.shutdownOutput();
					socket.close();
				}
			}
		} catch (IOException e) {
		}
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
	public int getValue() {
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
}
