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
import torrent.network.ByteInputStream;
import torrent.network.ByteOutputStream;
import torrent.protocol.BitTorrent;
import torrent.protocol.IMessage;
import torrent.protocol.MessageUtils;
import torrent.protocol.messages.MessageKeepAlive;
import torrent.protocol.messages.extention.MessageExtension;
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
		RESERVED_EXTENTION_BYTES[5] |= 0x10; // Extended Messages
		lastActivity = System.currentTimeMillis();
		passedHandshake = false;
	}

	public void connect() {
		try {
			setStatus("Connecting...");
			if (socket != null) {
				setStatus("Connected (Outside request)");
				return;
			}
			socket = new Socket();
			socket.connect(new InetSocketAddress(address, port), 4000);
			setName(toString());
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
			addToQueue(new MessageExtension(BitTorrent.EXTENDED_MESSAGE_HANDSHAKE, new MessageHandshake()));
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
			ThreadUtils.sleep(10);
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
			socket.close();
			crashed = true;
			return;
		} else {
			String protocol = inStream.readString(0x13);
			if ("BitTorrent protocol".equals(protocol)) {
				peerClient.setReservedBytes(inStream.readByteArray(8));
				byte[] torrentHash = inStream.readByteArray(20);
				if (torrentHash != torrent.getHashArray()) {
					inStream.readByteArray(20);
					setStatus("Awaiting Orders");
					passedHandshake = true;
				} else {
					log("Torrent Hash Mismatch: " + StringUtil.byteArrayToString(torrentHash), true);
					socket.close();
					crashed = true;
					return;
				}
			} else {
				log("Protocol Mismatch: " + protocol, true);
				crashed = true;
				socket.close();
				return;
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
			} catch (IOException e) {
				close();
			}
			ThreadUtils.sleep(1);
		}
		close();
		setStatus("Connection closed");
	}
	
	private void readMessage() throws IOException {
		if (inStream.available() > 0) {
			IMessage message = MessageUtils.getUtils().readMessage(inStream, this);
			message.process(this);
			lastActivity = System.currentTimeMillis();
		}
	}
	
	private void sendMessage() throws IOException {
		if (messageQueue.size() > 0) {
			IMessage message = messageQueue.remove(0);
			setStatus("Sending Message: " + message);
			MessageUtils.getUtils().writeMessage(outStream, message);
			setStatus("Sended Message: " + message);
			lastActivity = System.currentTimeMillis();
		}
	}
	
	public void checkDisconnect() {
		int inactiveSeconds = (int)((System.currentTimeMillis() - lastActivity) / 1000);
		if (inactiveSeconds > 10) {
			if(myClient.getQueueSize() > 0) { //We are not receiving a single byte in the last 30(!) seconds
				close();
				return;
			}
		}
		if (inactiveSeconds > 30 && torrent.getDownloadStatus() == Torrent.STATE_DOWNLOAD_DATA) {
			if (peerClient.hasPieceCount() == 0) { //They don't have anything
				if(torrent.getFiles().getPieceCount() == torrent.getFiles().getNeededPieces().size()) { //We can't send them anything
					close(); //We are of no use to eachother
					return;
				}
			}
		}
		if (inactiveSeconds > 60) { //We are waiting for something to happen by now
			if(myClient.isInterested() && myClient.isChoked()) { //They are not unchoking us
				close();
				return;
			}
		}
		if (inactiveSeconds > 90) {// 1.5 Minute, We are getting close to timeout D:
			if(myClient.isInterested()) {
				addToQueue(new MessageKeepAlive());
				return;
			}
		} 
		if (inactiveSeconds > 120) {// 2 Minutes, We've hit the timeout mark
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

	public void setStatus(String s) {
		status = s;
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
		return (getWorkQueueSize() >= myClient.getMaxRequests()) ?  0 : myClient.getMaxRequests() - getWorkQueueSize();
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
		if(inStream != null) {
			downloadRate = inStream.getSpeed();
			inStream.reset(downloadRate);
		}
		if(outStream != null) {
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
		return (getWorkQueueSize() * 5000) + peerClient.hasPieceCount() + downloadRate;
	}

}
