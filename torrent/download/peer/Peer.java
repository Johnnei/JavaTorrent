package torrent.download.peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import torrent.Logable;
import torrent.Manager;
import torrent.download.Torrent;
import torrent.download.files.Piece;
import torrent.network.ByteInputStream;
import torrent.network.ByteOutputStream;
import torrent.protocol.IMessage;
import torrent.protocol.MessageUtils;
import torrent.protocol.messages.MessageKeepAlive;
import torrent.protocol.messages.MessageRequest;
import torrent.protocol.messages.extention.MessageHandshake;
import torrent.util.ISortable;
import torrent.util.StringUtil;

public class Peer extends Thread implements Logable, ISortable {

	private byte[] RESERVED_EXTENTION_BYTES = new byte[8];

	private InetAddress address;
	private int port;

	private Torrent torrent;
	private Socket socket;
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
	 * The working queue, Remembers which pieces are requested<br/>
	 */
	private HashMap<Job, Integer> workingQueue;
	/**
	 * The maximum amount of simultaneous piece requests<br/>
	 * This is being corrected after every piece receive
	 */
	private int maxWorkload;
	/**
	 * The last time this connection showed any form of activity<br/>
	 * <i>Values are System.currentMillis()</i>
	 */
	private long lastActivity;

	private int downloadRate;
	private int uploadRate;
	private boolean passedHandshake;

	public Peer(Torrent torrent) {
		super("Peer Thread");
		this.torrent = torrent;
		crashed = false;
		peerClient = new Client();
		myClient = new Client();
		status = "";
		messageQueue = new ArrayList<>();
		workingQueue = new HashMap<Job, Integer>();
		RESERVED_EXTENTION_BYTES[5] |= 0x10; // Extended Messages
		lastActivity = System.currentTimeMillis();
		downloadRate = 0;
		uploadRate = 0;
		passedHandshake = false;
		maxWorkload = 1;
	}

	public void connect() {
		try {
			setStatus("Connecting...");
			if (socket != null) {
				setStatus("Connected (Outside request)");
				return;
			}
			socket = new Socket(address, port);
			inStream = new ByteInputStream(this, socket.getInputStream());
			outStream = new ByteOutputStream(socket.getOutputStream());
		} catch (IOException e) {
			crashed = true;
			return;
		}
		try {
			processHandshake();
		} catch (IOException e) {
			log("Connection crashed: " + e.getMessage(), true);
			crashed = true;
			return;
		}
	}

	private void sendExtentionMessage() throws IOException {
		if (peerClient.supportsExtention(5, 0x10)) { // EXTENDED_MESSAGE
			addToQueue(new MessageHandshake());
		}
	}

	public void setSocket(InetAddress address, int port) {
		this.address = address;
		this.port = port;
		super.setName("Peer " + address.getHostAddress() + ":" + port + " ");
	}

	public void processHandshake() throws IOException {
		setStatus("Sending Handshake"); //Not making this more OO because it's not within the message-flow
		outStream.writeByte(0x13);
		outStream.writeString("BitTorrent protocol");
		outStream.write(RESERVED_EXTENTION_BYTES);
		outStream.write(torrent.getHashArray());
		outStream.write(Manager.getPeerId());
		setStatus("Awaiting Handshake response");
		long handShakeSentTime = System.currentTimeMillis();
		while (inStream.available() < 68) {
			Torrent.sleep(10);
			if (System.currentTimeMillis() - handShakeSentTime > 5000) {
				if(inStream.available() == 0) {
					socket.close();
					return;
				}
			}
			if (System.currentTimeMillis() - handShakeSentTime > 30000) {
				log("Handshake error: " + inStream.available() + " bytes in 30 seconds", true);
				socket.close();
				return;
			}
		}
		status = "Verifying handshake";
		int protocolLength = inStream.read();
		if (protocolLength != 0x13) {
			log("Protocol Length Mismatch: " + protocolLength);
		} else {
			String protocol = inStream.readString(0x13);
			if ("BitTorrent protocol".equals(protocol)) {
				peerClient.setReservedBytes(inStream.readByteArray(8));
				byte[] torrentHash = inStream.readByteArray(20);
				if (torrentHash != torrent.getHashArray()) {
					inStream.readByteArray(20);
					setStatus("Awaiting Orders");
					passedHandshake = true;
					// if(peerId != stream.readByteArray(20))
					// log("Peer ID Mismatch: " +
					// Decoder.byteArrayToString(peerId) + " expected " +
					// Decoder.byteArrayToString(Manager.getPeerId()), true);
				} else {
					log("Torrent Hash Mismatch: " + StringUtil.byteArrayToString(torrentHash), true);
				}
			} else {
				log("Protocol Mismatch: " + protocol, true);
				crashed = true;
				socket.close();
			}
		}
		sendExtentionMessage();
	}

	@Override
	public void run() {
		connect();
		while (torrent.keepDownloading() && !closed()) {
			try {
				readMessage();
				sendMessage();
				checkDisconnect();
			} catch (IOException e) {
				close();
			}
			Torrent.sleep(1);
		}
		close();
		setStatus("Connection closed");
	}
	
	private void readMessage() throws IOException {
		if (inStream.available() > 0) {
			IMessage message = MessageUtils.getUtils().readMessage(inStream);
			message.process(this);
			lastActivity = System.currentTimeMillis();
		}
	}
	
	private void sendMessage() throws IOException {
		if (messageQueue.size() > 0) {
			MessageUtils.getUtils().writeMessage(outStream, messageQueue.remove(0));
			lastActivity = System.currentTimeMillis();
		}
	}
	
	private void checkDisconnect() {
		long millisSinceLastAction = System.currentTimeMillis() - lastActivity;
		if (millisSinceLastAction > 90000) {// 1.5 Minute
			if (workingQueue.size() > 0) {
				close();
			} else if (!myClient.isChoked()) {
				if(workingQueue.size() == 0) {
					addToQueue(new MessageKeepAlive());
				} else {
					close();
				}
			} else {
				close();
			}
		} else if (millisSinceLastAction > 120000) {// 2 Minutes
			close();
		}
	}

	/**
	 * Checks if the connection is/should be closed
	 * @return
	 * If the connection should be/is closed
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

	private void setStatus(String s) {
		status = s;
		setName(toString() + " " + status);
	}

	@Override
	public String getStatus() {
		return status;
	}

	public boolean hasPiece(int index) {
		return peerClient.hasPiece(index);
	}

	public void requestPiece(Piece piece) {
		int[] result = piece.getPieceRequest();
		if (result.length > 0) {
			MessageRequest m = new MessageRequest(piece.getIndex(), result[2], result[1]);
			messageQueue.add(m);
			workingQueue.put(new Job(piece.getIndex(), result[0]), 0);
			log("Requesting Piece: " + piece.getIndex() + "-" + result[0] + " (" + result[1] + " bytes)");
		} else {
			log("Ordered to request piece " + piece.getIndex() + " but it has no remaining sub-pieces!", true);
		}
	}

	public void requestMetadataPiece(int index) {
		log("Requesting Metadata Piece: " + index);
		torrent.protocol.messages.ut_metadata.MessageRequest mr = new torrent.protocol.messages.ut_metadata.MessageRequest(index);
		synchronized (this) {
			messageQueue.add(mr);
			workingQueue.put(new Job(-1 - index), 0);
		}
	}
	
	public void removeFromQueue(Job job) {
		workingQueue.remove(job);
	}

	public boolean hasExtentionId(String extention) {
		return peerClient.hasExtentionID(extention);
	}

	public boolean supportsExtention(int index, int bit) {
		return peerClient.supportsExtention(index, bit);
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
		return workingQueue.size() > 0;
	}
	
	public int getFreeWorkTime() {
		return (workingQueue.size() >= maxWorkload) ?  0 : maxWorkload - workingQueue.size();
	}

	public int getWorkQueue() {
		return workingQueue.size();
	}
	
	public int getMaxWorkLoad() {
		return maxWorkload;
	}

	public void addToQueue(IMessage m) {
		messageQueue.add(m);
	}

	public int hasPieceCount() {
		return peerClient.hasPieceCount();
	}

	public Client getClient() {
		return peerClient;
	}

	public Client getMyClient() {
		return myClient;
	}

	public void pollRates() {
		downloadRate = (inStream == null) ? 0 : inStream.getSpeedAndReset();
		uploadRate = (outStream == null) ? 0 : outStream.getSpeedAndReset();
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
		if (workingQueue.size() > 0) {
			Object[] keys = workingQueue.keySet().toArray();
			for (int i = 0; i < keys.length; i++) {
				Job job = (Job) keys[i];
				torrent.cancelPiece(job.getPieceIndex(), job.getSubpiece());
			}
		}
	}

	public void close() {
		try {
			if (socket != null) {
				if(!socket.isClosed())
					socket.close();
			}
		} catch (IOException e) {
		}
		cancelAllPieces();
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
		return getWorkQueue();
	}

}
