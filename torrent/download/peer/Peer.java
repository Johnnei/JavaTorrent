package torrent.download.peer;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

import torrent.JavaTorrent;
import torrent.Logable;
import torrent.Manager;
import torrent.download.Torrent;
import torrent.download.files.Piece;
import torrent.encoding.Bencode;
import torrent.encoding.Bencoder;
import torrent.network.ByteInputStream;
import torrent.network.ByteOutputStream;
import torrent.network.Message;
import torrent.network.PieceRequest;
import torrent.network.Stream;
import torrent.protocol.BitTorrent;
import torrent.protocol.UTMetadata;
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
	 * The peer on which we are connected his id
	 */
	private byte[] peerId;
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
	private ArrayList<Message> messageQueue;
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
		messageQueue = new ArrayList<Message>();
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
			Bencoder encoder = new Bencoder();
			encoder.dictionaryStart();
			encoder.string("m");
			encoder.dictionaryStart();
			encoder.string("ut_metadata");
			encoder.integer(UTMetadata.EXTENDED_MESSAGE_UT_METADATA);
			encoder.dictionaryEnd();
			encoder.string("v");
			encoder.string(JavaTorrent.BUILD);
			encoder.dictionaryEnd();
			String bencoded = encoder.getBencodedData();
			Message message = new Message(2 + bencoded.length());
			message.getStream().writeByte(BitTorrent.MESSAGE_EXTENDED_MESSAGE);
			message.getStream().writeByte(BitTorrent.EXTENDED_MESSAGE_HANDSHAKE);
			message.getStream().writeString(bencoded);
			outStream.write(message.getStream().getBuffer());
		}
	}

	public void setSocket(InetAddress address, int port) {
		this.address = address;
		this.port = port;
		super.setName("Peer " + address.getHostAddress() + ":" + port + " ");
	}

	public void processHandshake() throws IOException {
		setStatus("Sending Handshake");
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
					peerId = inStream.readByteArray(20);
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
			int length = inStream.readInt();
			if (length == 0) {
				// Keep-Alive
			} else {
				int messageId = inStream.readByte();
				length -= 1;
				Stream stream = new Stream(length);
				long readDuration = System.currentTimeMillis();
				if (length > 0) {
					stream.fill(inStream.readByteArray(length));
					readDuration = System.currentTimeMillis() - readDuration;
				}
				switch (messageId) {
				case BitTorrent.MESSAGE_CHOKE:
					myClient.choke();
					cancelAllPieces();
					break;

				case BitTorrent.MESSAGE_UNCHOKE:
					myClient.unchoke();
					break;

				case BitTorrent.MESSAGE_INTERESTED:
					myClient.interested();
					break;

				case BitTorrent.MESSAGE_UNINTERESTED:
					myClient.uninterested();
					break;

				case BitTorrent.MESSAGE_HAVE:
					peerClient.addPiece(stream.readInt());
					break;

				case BitTorrent.MESSAGE_BITFIELD:
					int pieceIndex = 0;
					while (stream.available() > 0) {
						int b = stream.readByte();
						for (int bit = 0; bit < 8; bit++) {
							if (((b >> bit) & 1) == 1) {
								peerClient.addPiece(pieceIndex);
							}
							pieceIndex++;
						}
					}
					break;

				case BitTorrent.MESSAGE_REQUEST:
					PieceRequest pr = new PieceRequest(stream.readInt(), stream.readInt(), stream.readInt());
					peerClient.requesting(pr);
					break;

				case BitTorrent.MESSAGE_PIECE: {
					int index = stream.readInt();
					int offset = stream.readInt();
					byte[] data = stream.readByteArray(stream.available());
					synchronized(this) {
						torrent.collectPiece(index, offset, data);
						workingQueue.remove(new Job(index, torrent.getTorrentFiles().getBlockIndexByOffset(offset)));
					}
					if(readDuration > 30000 || readDuration < 1) {
						maxWorkload = 1;
					} else if(readDuration >= 1000) {
						maxWorkload = 2;
					} else {
						maxWorkload = (maxWorkload + (int)Math.ceil(1000D / readDuration)) / 2;
					}
					break;
				}

				case BitTorrent.MESSAGE_EXTENDED_MESSAGE:
					int extendedMessageId = stream.readByte();
					length -= 1;
					switch (extendedMessageId) {
					case BitTorrent.EXTENDED_MESSAGE_HANDSHAKE: {
						String bencoded = stream.readString(length);
						Bencode bencoding = new Bencode(bencoded);
						HashMap<String, Object> dictionary = bencoding.decodeDictionary();
						Object o = dictionary.get("m");
						if (o == null) {
							log("Error: M Dictionary is missing", true);
						} else {
							HashMap<String, Object> data = (HashMap<String, Object>) o;
							if (data.containsKey("ut_metadata")) {
								peerClient.addExtentionID("ut_metadata", (int) data.get("ut_metadata"));
								if(dictionary.get("metadata_size") != null)
									torrent.getMetadata().setFilesize((int) dictionary.get("metadata_size"));
							}
						}
						break;
					}

					case UTMetadata.EXTENDED_MESSAGE_UT_METADATA: {

						Bencode decoder = new Bencode(stream.readString(length));
						HashMap<String, Object> dictionary = decoder.decodeDictionary();

						switch ((int) dictionary.get("msg_type")) {
						case UTMetadata.UT_METADATA_REQUEST:
							Bencoder encode = new Bencoder();
							encode.dictionaryStart();
							encode.string("msg_type");
							encode.integer(UTMetadata.UT_METADATA_REJECT);
							encode.string("piece");
							encode.integer((int) dictionary.get("piece"));
							encode.dictionaryEnd();
							String bencodedReject = encode.getBencodedData();
							Message m = new Message(2 + bencodedReject.length());
							m.getStream().writeByte(BitTorrent.MESSAGE_EXTENDED_MESSAGE);
							m.getStream().writeByte(peerClient.getExtentionID("ut_metadata"));
							m.getStream().writeString(bencodedReject);
							messageQueue.add(m);
							break;

						case UTMetadata.UT_METADATA_DATA:
							if ((int) dictionary.get("total_size") == torrent.getMetadata().getTotalSize()) {
								stream.moveBack(decoder.remainingChars());
								byte[] data = stream.readByteArray(stream.available());
								synchronized (this) {
									torrent.collectPiece((int) dictionary.get("piece"), data);
									workingQueue.remove(new Job(-1 - (int)dictionary.get("piece")));
								}
							} else {
								log("Piece Request size check failed: " + dictionary.get("total_size"), true);
								synchronized (this) {
									torrent.collectPiece((int) dictionary.get("piece"), null);
								}
							}
							break;

						case UTMetadata.UT_METADATA_REJECT:
							log("Piece Request got rejected: " + dictionary.get("piece"), true);
							torrent.collectPiece((int) dictionary.get("piece"), null);
							break;
						}

					}
						break;
					}
					break;

				default:
					log("Unhandled message: " + messageId, true);
					break;
				}
			}
			lastActivity = System.currentTimeMillis();
		}
	}
	
	private void sendMessage() throws IOException {
		if (messageQueue.size() > 0) {
			Message message = messageQueue.remove(0);
			outStream.write(message.getMessage());
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
					addToQueue(new Message(0));
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
		Message m = new Message(13);
		m.getStream().writeByte(BitTorrent.MESSAGE_REQUEST);
		int[] result = piece.fillPieceRequest(m);
		if (result.length > 0) {
			synchronized(this) {
				messageQueue.add(m);
				workingQueue.put(new Job(piece.getIndex(), result[0]), 0);
			}
			log("Requesting Piece: " + piece.getIndex() + "-" + result[0] + " (" + result[1] + " bytes)");
		} else {
			log("Ordered to request piece " + piece.getIndex() + " but it has no remaining sub-pieces!", true);
		}
	}

	public void requestMetadataPiece(int index) {
		log("Requesting Metadata Piece: " + index);
		Bencoder encoder = new Bencoder();
		encoder.dictionaryStart();
		encoder.string("msg_type");
		encoder.integer(UTMetadata.UT_METADATA_REQUEST);
		encoder.string("piece");
		encoder.integer(index);
		encoder.dictionaryEnd();
		String bencoded = encoder.getBencodedData();
		Message m = new Message(2 + bencoded.length());
		m.getStream().writeByte(BitTorrent.MESSAGE_EXTENDED_MESSAGE);
		m.getStream().writeByte(peerClient.getExtentionID("ut_metadata"));
		m.getStream().writeString(bencoded);
		synchronized (this) {
			messageQueue.add(m);
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

	public void addToQueue(Message m) {
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
