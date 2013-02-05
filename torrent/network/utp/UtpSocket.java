package torrent.network.utp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Random;

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
	 * The number of bytes which have not yet been acked
	 */
	private int cur_window;
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
	 * The messages which have not yet been acked
	 */
	private ArrayList<UtpMessage> messagesInFlight;
	/**
	 * The messages which have not yet been send by limitations
	 */
	private ArrayList<UtpMessage> messageQueue;
	/**
	 * The window in which messages have to arrive before being "lost"
	 */
	private int timeout;
	private SocketAddress remoteAddress;

	/**
	 * Creates a new uTP socket
	 */
	public UtpSocket() {
		utpEnabled = true;
		cur_window = 0;
		utpConnectionState = ConnectionState.PENDING;
		packetSize = 150;
		wnd_size = 150;
		seq_nr = 1;
		utpBuffer = new Stream(5000);
		connection_id_recv = NO_CONNECTION;
		connection_id_send = NO_CONNECTION;
		messagesInFlight = new ArrayList<>();
		messageQueue = new ArrayList<>();
	}

	/**
	 * Creates a new uTP socket
	 */
	public UtpSocket(boolean utpEnabled) {
		this();
		this.utpEnabled = utpEnabled;
	}
	
	public UtpSocket(SocketAddress address, boolean utpEnabled) {
		this(utpEnabled);
		remoteAddress = address;
	}

	/**
	 * Gets called if the socket got accepted from outside the client to set the remoteAddress correctly
	 */
	public void accepted() throws IOException {
		remoteAddress = getRemoteSocketAddress();
		socket = new DatagramSocket(remoteAddress);
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
		socket = new DatagramSocket(getPort());
		remoteAddress = address;
		this.timeout = timeout;
		utpConnectionState = ConnectionState.CONNECTING;
		connection_id_recv = new Random().nextInt() & 0xFFFF;
		connection_id_send = connection_id_recv + 1;
		System.out.println("Connecting uTP to " + address + " connId: " + connection_id_recv);
		UtpMessage synMessage = new UtpMessage(connection_id_recv, cur_window, ST_SYN, seq_nr++, 0);
		write(synMessage);
		long sendTime = System.currentTimeMillis();
		int tries = 1;
		byte[] response = null;
		while (response == null) {
			if (System.currentTimeMillis() - sendTime >= 1000) { // Timeout
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
					System.out.println("Connected to " + address);
					receive(response);
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
		if (getBytesInFlight() + message.getSize() < wnd_size) {
			return write(message);
		} else {
			if(!messageQueue.contains(message)) {
				messageQueue.add(message);
			}
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
			DatagramPacket packet = new DatagramPacket(data, data.length, remoteAddress);
			socket.send(packet);
			messagesInFlight.add(message);
			System.out.println("[uTP] Wrote message: " + (data[0] >>> 4));
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
	 */
	private void receive(byte[] dataBuffer) {
		receive(dataBuffer, dataBuffer.length);
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
		System.out.println("Received UDP Message: " + type + " (Version " + version + "), Size: " + length);
		int extension = data.readByte();
		int connection_id = data.readShort();
		long timestamp = (long) data.readInt();
		long timestampDiff = (long) data.readInt();
		long windowSize = data.readInt();
		short sequenceNumber = (short)data.readShort();
		short ackNumber = (short)data.readShort();
		System.out.println("Extension: " + extension);
		System.out.println("Connection ID: " + connection_id);
		System.out.println("timestampc " + (getCurrentMicroseconds() & 0xFFFFFFFFL));
		System.out.println("timestamp: " + timestamp);
		System.out.println("timestampDiff: " + timestampDiff);
		System.out.println("windowSize: " + windowSize);
		System.out.println("sequenceNumber: " + sequenceNumber);
		System.out.println("ackNumber: " + ackNumber);
		if (extension != 0) {
			while (data.available() > 0) {
				extension = data.readByte();
				int extensionLength = data.readByte();
				System.out.println("Extension " + extension + " of length " + extensionLength);
				data.moveBack(-extensionLength);
			}
		}
		measured_delay = getCurrentMicroseconds() - timestamp;
		System.out.println("Message Delay: " + measured_delay);
		// Process Data
		switch (type) {
			case ST_SYN: { // Connect
				System.out.println("[uTP Protocol] SYN");
				if(utpConnectionState == ConnectionState.CONNECTED) {
					System.out.println("[uTP Protocol] Ignored SYN on uTP Connected Socket");
					return;
				}
				connection_id_send = connection_id;
				connection_id_recv = connection_id + 1;
				seq_nr = (short)(new Random().nextInt() & 0xFFFF);
				ack_nr = ackNumber;
				utpConnectionState = ConnectionState.CONNECTED;
				utpEnabled = true;
				System.out.println("[uTP] Connected");
				messageQueue.add(new UtpMessage(this, ST_STATE, sequenceNumber, ackNumber));
				break;
			}
			
			case ST_STATE: { // ACK
				System.out.println("[uTP Protocol] STATE");
				messagesInFlight.remove(new UtpMessage(sequenceNumber));
				break;
			}
			
			case ST_DATA: { // Data
				System.out.println("[uTP Protocol] DATA");
				break;
			}
			
			case ST_FIN: { // Disconenct
				System.out.println("[uTP Protocol] FIN");
				break;
			}
			
			case ST_RESET: { //Crash
				System.out.println("[uTP Protocol] RESET");
				break;
			}
			
			default: {
				System.err.println("[uTP Protocol] Unkown type: " + type);
				break;
			}
		}
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
	 * Checks if we need to try to send messages
	 */
	public void checkForSendingPackets() {
		while(messageQueue.size() > 0) {
			UtpMessage message = messageQueue.get(0);
			if(send(messageQueue.get(0))) {
				messageQueue.remove(message);
			} else {
				break;
			}
		}
	}

	/**
	 * Checks the connection if there are pending messages
	 */
	public void checkForPackets() {
		byte[] data = Manager.getManager().getUdpMultiplexer().accept(remoteAddress, connection_id_recv);
		if (data != null) {
			receive(data);
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
		return System.currentTimeMillis() * 1000;
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
		if(super.isClosed()) {
			return new ByteInputStream(this, peer, null);
		} else {
			return new ByteInputStream(this, peer, getInputStream());
		}
	}

	public ByteOutputStream getOutputStream() throws IOException {
		if(super.isClosed()) {
			return new ByteOutputStream(this, null);
		} else {
			return new ByteOutputStream(this, super.getOutputStream());
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
		return wnd_size;
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
