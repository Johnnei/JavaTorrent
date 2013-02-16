package torrent.network.utp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;

import org.johnnei.utils.JMath;
import org.johnnei.utils.ThreadUtils;

import torrent.Manager;
import torrent.download.peer.Peer;
import torrent.network.ByteInputStream;
import torrent.network.ByteOutputStream;
import torrent.network.Stream;

/**
 * The uTorrent Transport Protocol Socket.<br/>
 * The socket supports: TCP and uTP.<br/>
 * The socket will by default send data over TCP until the switch is being hit to send via uTP.
 * 
 * @author Johnnei
 * 
 */
public class UtpSocket extends Socket {

	/**
	 * The implemented version of the uTP Protocol
	 */
	public static final byte VERSION = 1;
	/**
	 * Regular Data packet<br/>
	 * Must have data payload
	 */
	public static final byte ST_DATA = 0;
	/**
	 * Finalize Packet<br/>
	 * Closes the socket in a formal manner<br/>
	 * The seq_nr of this packet will be the eof_pkt and no seq_nr will become higher.<br/>
	 * The other end may wait for missing/ out of order packets which are still pending and lower than eof_pkt
	 */
	public static final byte ST_FIN = 1;
	/**
	 * State Packet<br/>
	 * A packet which is used to send ACK messages whilst there is no data to be send along with it<br/>
	 * This packet does not increase {@link #seq_nr}
	 */
	public static final byte ST_STATE = 2;
	/**
	 * Reset Packet<br/>
	 * Abruptly closes the socket<br/>
	 */
	public static final byte ST_RESET = 3;
	/**
	 * Connect Packet<br/>
	 * {@link #seq_nr} is initialized to 1<br/>
	 * {@link #connection_id_recv} is initialized to a random number.<br/>
	 * {@link #connection_id_send} is initialized to {@link #connection_id_recv} + 1<br/>
	 * This message is send with {@link #connection_id_recv} instead of {@link #connection_id_send}<br/>
	 * The other end will copy the received id
	 */
	public static final byte ST_SYN = 4;
	/**
	 * The connectionId for sockets which have not yet been initialised
	 */
	public static final int NO_CONNECTION = Integer.MAX_VALUE;
	/**
	 * This is the default state for either:<br/>
	 * TCP Connection or uTP is being initialized
	 */
	/**
	 * UDP Socket
	 */
	private DatagramSocket socket;

	/**
	 * The maximum amount of bytes which have not yet been acked
	 */
	private int max_window;
	/**
	 * The {@link #max_window} as received from the other end.<br/>
	 * This sets the upper limit for the bytes which have not yet been acked
	 */
	private int wnd_size;
	/**
	 * Our 16-bit connection id
	 */
	private int connection_id_send;
	/**
	 * Their 16-bit connection id.<br/>
	 * This should be {@link #connection_id_send} value + 1
	 */
	private int connection_id_recv;
	/**
	 * The next packet id
	 */
	private short seq_nr;
	/**
	 * The last received packet id
	 */
	private short ack_nr;
	/**
	 * The last measured delay on this socket
	 */
	private long measured_delay;
	/**
	 * The state of the uTP Connection.<br/>
	 * For {@link #ST_RESET} is (as specified) no state
	 */
	private ConnectionState utpConnectionState;
	/**
	 * The size of each packet which send on the stream
	 */
	private int packetSize;
	/**
	 * The current state of the Socket<br/>
	 * If true: uTP is being used<br/>
	 * If false <i>default</i>: TCP is being used
	 */
	private boolean utpEnabled;
	/**
	 * A buffer to store data blocks in
	 */
	private Stream utpBuffer;
	/**
	 * A buffer to store data in which will be send in a larger chunk so we can match {@link #packetSize}
	 */
	private Stream sendBuffer;
	/**
	 * The last time at which the send buffer has been altered<br/>
	 * If the last update is larger than 10ms then the {@link #sendBuffer} will be send so nothing will get stuck
	 */
	private long lastSendBufferUpdate;
	/**
	 * The messages which have not yet been acked
	 */
	private ArrayList<UtpMessage> messagesInFlight;
	/**
	 * The messages which have not yet been send by limitations
	 */
	private LinkedList<UtpMessage> messageQueue;
	/**
	 * The window in which messages have to arrive before being "lost"
	 */
	private int timeout;
	/**
	 * The RTT measured on this socket
	 */
	private int roundTripTime;
	/**
	 * The RTT Variance measured on this socket
	 */
	private int roundTripTimeVariance;
	private SocketAddress remoteAddress;

	/**
	 * Creates a new uTP socket
	 */
	public UtpSocket() throws IOException {
		utpEnabled = true;
		utpConnectionState = ConnectionState.PENDING;
		packetSize = 150;
		max_window = wnd_size = 150;
		seq_nr = 1;
		timeout = 1000;
		utpBuffer = new Stream(5000);
		sendBuffer = new Stream(packetSize);
		connection_id_recv = NO_CONNECTION;
		connection_id_send = NO_CONNECTION;
		messagesInFlight = new ArrayList<>();
		messageQueue = new LinkedList<>();
		socket = new DatagramSocket();
	}

	/**
	 * Creates a new uTP socket
	 */
	public UtpSocket(boolean utpEnabled) throws IOException {
		this();
		this.utpEnabled = utpEnabled;
	}
	
	public UtpSocket(SocketAddress address, boolean utpEnabled) throws IOException {
		this(utpEnabled);
		remoteAddress = address;
	}

	/**
	 * Gets called if the socket got accepted from outside the client to set the remoteAddress correctly
	 */
	public void accepted() throws IOException {
		remoteAddress = getRemoteSocketAddress();
	}

	/**
	 * Connects to the TCP Socket
	 * 
	 * @throws IOException
	 */
	public void connect() throws IOException {
		System.out.println("Connecting TCP to " + remoteAddress);
		utpConnectionState = ConnectionState.DISCONNECTED;
		utpEnabled = false;
		super.connect(remoteAddress, 1000);
		System.out.println("Connected");
	}

	/**
	 * Connects to the uTP Socket and prepares the UDP Socket
	 * 
	 * @param address The address to connect to
	 */
	public void connect(SocketAddress address) throws IOException {
		connect(address, 1000);
	}

	/**
	 * Connects to the uTP Socket and prepares the UDP Socket
	 * 
	 * @param address The address to connect to
	 * @param timeout The maximum amount of miliseconds this connect attempt may take
	 */
	public void connect(SocketAddress address, int timeout) throws IOException {
		socket = new DatagramSocket();
		remoteAddress = address;
		utpConnectionState = ConnectionState.CONNECTING;
		connection_id_recv = new Random().nextInt() & 0xFFFF;
		connection_id_send = connection_id_recv + 1;
		System.out.println("[uTP Protocol] Connecting to " + address + " connId: " + connection_id_recv);
		UtpMessage synMessage = new UtpMessage(connection_id_recv, max_window, ST_SYN, seq_nr++, 0);
		write(synMessage);
		long sendTime = System.currentTimeMillis();
		int tries = 1;
		byte[] response = null;
		while (response == null) {
			if (System.currentTimeMillis() - sendTime >= timeout) { // Timeout
				write(synMessage);
				sendTime = System.currentTimeMillis();
				if (++tries == 3) {
					connect();
					break;
				}
			}
			response = Manager.getManager().getUdpMultiplexer().accept(remoteAddress, connection_id_recv);
			if (response == null) {
				ThreadUtils.sleep(100);
			} else {
				if (response[0] >>> 4 != ST_STATE) {// Check Type
					System.err.println("Invalid SYN Response: " + (response[0] >>> 4));
					connect();
				} else {
					System.out.println("[uTP Protocol] Connected to " + address);
					receive(response, response.length);
					utpConnectionState = ConnectionState.CONNECTED;
				}
			}
		}
	}

	/**
	 * Tries to send a uTP Message, If the window is to small it will get queued<br/>
	 * If the packet got queued it will return false<br/>
	 * It will not requeue messages which are already in the queue
	 * 
	 * @param message the UtpMessage which should be attempted to write
	 * @return true if the message has been send else false
	 */
	public boolean send(UtpMessage message) {
		System.out.println("Checking send of a " + message.getSize() + " bytes of type " + message.getType());
		if (getBytesInFlight() + message.getSize() < wnd_size) {
			return write(message);
		} else {
			messageQueue.addLast(message);
			return false;
		}
	}

	/**
	 * Sends a UDP Message
	 * 
	 * @return true if the packet got sended
	 */
	private boolean write(UtpMessage message) {
		message.setTimestamp(this);
		byte[] data = message.getData();
		try {
			Manager.getManager().getUdpMultiplexer().send(new DatagramPacket(data, data.length, remoteAddress));
			if(message.getType() == ST_DATA) { //We don't expect ACK's on anything but DATA
				messagesInFlight.add(message);
			}
			System.out.println("[uTP] Wrote message: " + (data[0] >>> 4) + " to " + remoteAddress + ", seq_nr: " + message.hashCode());
			return true;
		} catch (IOException e) {
			System.err.println("[uTP] Failed to send message: " + e.getMessage());
			return false;
		}
	}
	
	/**
	 * Accept a UDP Packet from the UDP Multiplexer
	 * 
	 * @param dataBuffer The data buffer used in the packet
	 * @param length The amount of bytes received in the packet
	 */
	public void receive(byte[] dataBuffer, int length) {
		if (length < 20) {
			System.err.println("UDP Packet to small for header: " + length + " bytes");
			return;
		}
		Stream data = new Stream(dataBuffer);
		int versionAndType = data.readByte();
		int version = versionAndType & 0x7;
		int type = versionAndType >>> 4;
		int extension = data.readByte() & 0xFF;
		int connection_id = data.readShort() & 0xFFFF;
		long timestamp = (long)(data.readInt() & 0xFFFFFFFFL);
		long timestampDiff = (long)(data.readInt() & 0xFFFFFFFFL);
		long windowSize = data.readInt() & 0xFFFFFFFFL;
		int sequenceNumber = data.readShort() & 0xFFFF;
		short ackNumber = (short)data.readShort();
		System.out.println("Received UDP Message: " + type + " (Version " + version + "), Size: " + length + ", Connection ID: " + connection_id);
		System.out.println("Extension: " + extension + ", windowSize: " + windowSize);
		System.out.println("timestamp: " + timestamp + ", Diff: " + timestampDiff + ", (CNano: " + System.nanoTime() + "), CMicro: " + (getCurrentMicroseconds() & 0xFFFFFFFFL));
		System.out.println("seq_nr: " + sequenceNumber + ", ack_nr: " + (ackNumber & 0xFFFF));
		if (extension != 0) {
			while (data.available() > 0) {
				extension = data.readByte();
				int extensionLength = data.readByte();
				System.out.println("Extension " + extension + " of length " + extensionLength);
				for(int i = 0; i < extensionLength; i++) {
					System.out.print(Integer.toHexString(data.readByte()) + " ");
				}
				System.out.println();
			}
		}
		measured_delay = ((getCurrentMicroseconds() & 0xFFFFFFFFL) - timestamp) / 1000;
		ack_nr = ackNumber;
		processACK(ack_nr);
		System.out.println("Message Delay: " + measured_delay + "ms");
		// Process Data
		switch (type) {
			case ST_SYN: { // Connect
				System.out.println("[uTP Protocol] SYN");
				if(utpConnectionState == ConnectionState.CONNECTED) {
					messageQueue.addLast(new UtpMessage(this, ST_STATE, seq_nr, ackNumber));
					System.out.println("[uTP Protocol] Ignored SYN on uTP Connected Socket. Resending SYN ACK");
					return;
				}
				connection_id_send = connection_id;
				connection_id_recv = connection_id + 1;
				seq_nr = (short)(new Random().nextInt() & 0xFFFF);
				ack_nr = (short)ackNumber;
				utpConnectionState = ConnectionState.CONNECTED;
				utpEnabled = true;
				System.out.println("[uTP] Connected");
				messageQueue.addLast(new UtpMessage(this, ST_STATE, seq_nr, ackNumber));
				break;
			}
			
			case ST_STATE: { // ACK
				System.out.println("[uTP Protocol] STATE");
				break;
			}
			
			case ST_DATA: { // Data
				System.err.println("[uTP Protocol] DATA");
				storeData(dataBuffer, data.getReadPointer(), data.available());
				break;
			}
			
			case ST_FIN: { // Disconenct
				System.out.println("[uTP Protocol] FIN");
				write(new UtpMessage(this, ST_FIN, seq_nr, ack_nr));
				utpConnectionState = ConnectionState.DISCONNECTED;
				break;
			}
			
			case ST_RESET: { //Crash
				System.out.println("[uTP Protocol] RESET");
				utpConnectionState = ConnectionState.PENDING;
				utpEnabled = false;
				break;
			}
			
			default: {
				System.err.println("[uTP Protocol] Unkown type: " + type);
				break;
			}
		}
	}
	
	private void processACK(int ackNumber) {
		UtpMessage message = null;
		for(int i = 0; i < messagesInFlight.size(); i++) {
			if(messagesInFlight.get(i).equals(new UtpMessage(ackNumber))) {
				message = messagesInFlight.remove(i);
				break;
			}
		}
		if(message != null) { //If this message hasn't been ack'ed before
			long rtt = getCurrentMicroseconds() - message.getSendTime();
			long delta = roundTripTime - rtt;
			roundTripTimeVariance += (JMath.abs(delta) - roundTripTimeVariance) / 4;
			roundTripTime = (int)(rtt - roundTripTime) / 8;
			setTimeout(roundTripTime + roundTripTimeVariance * 4);
			System.out.println("RTT: " + roundTripTime + ", Packet RTT: " + rtt);
			System.out.println("RTTVar: " + roundTripTimeVariance + ", Packet RTTV: " + delta);
		}
	}
	
	/**
	 * Sets the max_window in bytes to the larger of <tt>window</tt> or 150
	 * @param window The new max_window
	 */
	private void setMaxWindow(int window) {
		max_window = JMath.max(window, 150);
	}
	
	/**
	 * Sets the timeout in milliseconds to the larger of <tt>timeout</tt> or 500
	 * @param timeout The new timeout
	 */
	private void setTimeout(int timeout) {
		timeout = JMath.max(timeout, 500);
	}

	/**
	 * Store the filtered data in the output buffer<br/>
	 * This data will be available for the client to read
	 * 
	 * @param dataBuffer The buffer containing all data
	 * @param offset The start offset to start reading
	 * @param length The amount of bytes to be read
	 */
	private void storeData(byte[] dataBuffer, int offset, int length) {
		if (utpBuffer.writeableSpace() >= length) {
			System.err.println("Storing " + length + " bytes");
			for (int i = 0; i < length; i++) {
				utpBuffer.writeByte(dataBuffer[offset + i]);
			}
		} else {
			utpBuffer.refit(); // Try to reshape the buffer so we can append the data
			if (utpBuffer.writeableSpace() < length) { // Still not enough so we expand the buffer
				utpBuffer.expand(length - utpBuffer.writeableSpace());
			}
			storeData(dataBuffer, offset, length);
		}
	}
	
	/**
	 * Writes a byte array on the uTP Stream
	 * @param b The array of bytes to write
	 */
	public void write(byte b[]) {
		int writtenBytes = 0;
		while(writtenBytes < b.length) {
			int bytesToWrite = JMath.min(b.length - writtenBytes, sendBuffer.writeableSpace());
			if(bytesToWrite == 0) { //sendBuffer is full
				sendBuffer();
			} else {
				sendBuffer.writeByte(b, writtenBytes, bytesToWrite);
				writtenBytes += bytesToWrite;
				lastSendBufferUpdate = System.currentTimeMillis();
			}
		}
	}
	
	/**
	 * Writes a byte on the uTP Stream
	 * @param i The byte
	 */
	public void write(int i) {
		if(sendBuffer.writeableSpace() > 0) {
			sendBuffer.writeByte(i);
			lastSendBufferUpdate = System.currentTimeMillis();
		} else {
			sendBuffer();
			sendBuffer.writeByte(i);
		}
	}
	
	/**
	 * Tries to send the {@link #sendBuffer}
	 */
	private void sendBuffer() {
		UtpMessage message = new UtpMessage(this, ST_DATA, seq_nr++, ack_nr, sendBuffer.getWrittenBuffer());
		messageQueue.addLast(message);
		sendBuffer.resetWritePointer();
	}
	
	/**
	 * Checks if we need to try to send messages
	 */
	public void checkForSendingPackets() {
		if(System.currentTimeMillis() - lastSendBufferUpdate > 10) {
			if(sendBuffer.getWritePointer() > 0) {
				sendBuffer();
			}
		}
		while(messageQueue.size() > 0) {
			UtpMessage message = messageQueue.getFirst();
			if(send(message)) {
				messageQueue.removeFirst();
			} else {
				break;
			}
		}
		//Resend ack
		for(int i = 0; i < messagesInFlight.size(); i++) {
			UtpMessage message = messagesInFlight.get(i);
			long delay = getDelay(message, false);
			if(delay > timeout) {
				write(message);
				System.out.println("[uTP] Resending Message (SendTime: " + message.getSendTime() + ", Timeout: " + delay +")");
			}
		}
	}
	
	/**
	 * Calculates the delay from a message
	 * 
	 * @param message The message to compare with
	 * @param usPrecision If it should be microsecond precision instead of milisecond
	 * @return the delay 
	 */
	private long getDelay(UtpMessage message, boolean msPrecision) {
		long cTime = (int)getCurrentMicroseconds() & 0xFFFFFFFFL;
		long messageTime = message.getSendTime();
		if(msPrecision)
			return cTime - messageTime;
		else
			return (cTime - messageTime) / 1000;
	}

	/**
	 * Checks the connection if there are pending messages
	 */
	public void checkForPackets() {
		byte[] data = Manager.getManager().getUdpMultiplexer().accept(remoteAddress, connection_id_recv);
		if (data != null) {
			receive(data, data.length);
		}
	}

	/**
	 * Calculates the amount of bytes which have not yet been acked
	 * 
	 * @return
	 */
	private int getBytesInFlight() {
		int bytes = 0;
		for (int i = 0; i < messagesInFlight.size(); i++) {
			bytes += messagesInFlight.get(i).getSize();
		}
		return bytes;
	}

	/**
	 * Gets the current microseconds<br/>
	 * The problem is that java does not have a precise enough system so it's just the currentMillis * 1000<br/>
	 * But there is nanoSeconds bla bla, Read the javadocs please
	 * 
	 * @return
	 */
	public long getCurrentMicroseconds() {
		return (System.currentTimeMillis() & 0xFFFFFFFFL) * 1000L;//Strip to 32-bit precision
	}

	/**
	 * Switches the stream to TCP<br/>
	 * 
	 */
	public void disableUTP() throws IOException {
		utpEnabled = false;
		utpConnectionState = ConnectionState.CONNECTING;
	}

	public boolean isClosed() {
		if (utpEnabled) {
			return utpConnectionState == ConnectionState.DISCONNECTED;
		} else {
			if (socket == null)
				return false; // Else it won't even connect
			return socket.isClosed();
		}
	}

	/**
	 * Creates a new ByteInputStream for this socket
	 * 
	 * @param peer The associated peer
	 * @return The inputStream for this socket
	 * @throws IOException
	 */
	public ByteInputStream getInputStream(Peer peer) throws IOException {
		if(!super.isConnected()) {
			return new ByteInputStream(this, peer, null);
		} else {
			return new ByteInputStream(this, peer, getInputStream());
		}
	}

	public ByteOutputStream getOutputStream() throws IOException {
		if(!super.isConnected()) {
			return new ByteOutputStream(this, null);
		} else {
			return new ByteOutputStream(this, super.getOutputStream());
		}
	}
	
	@Override
	public String toString() {
		if(!super.isConnected()) {
			return remoteAddress.toString().substring(1);
		} else {
			return super.getInetAddress().toString().substring(1);
		}
	}
	
	@Override
	public int getPort() {
		if(!super.isConnected()) {
			return 0;
		} else {
			return super.getPort();
		}
	}

	/**
	 * Gets the last measured delay on the socket
	 * 
	 * @return The delay on this socket in microseconds
	 */
	public long getDelay() {
		return measured_delay;
	}

	/**
	 * Gets the connection ID which we use to sign messages with
	 * 
	 * @return
	 */
	public int getConnectionId() {
		return connection_id_send;
	}

	/**
	 * Gets the current window size which we want to advertise
	 * 
	 * @return
	 */
	public int getWindowSize() {
		return max_window;
	}

	/**
	 * Gets the available data on the stream
	 * 
	 * @return The stream containing the available data
	 */
	public Stream getStream() {
		return utpBuffer;
	}

	/**
	 * Checks if the stream has switched to uTP Mode
	 * 
	 * @return true if the uTP mode has been enabled
	 */
	public boolean isUTP() {
		return utpEnabled;
	}
}
