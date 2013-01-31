package torrent.network.utp;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.ArrayList;

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
	private int seq_nr;
	/**
	 * The last received packet id
	 */
	private int ack_nr;
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
	 * Creates a new uTP socket
	 */
	public UtpSocket() {
		utpEnabled = true;
		utpConnectionState = ConnectionState.PENDING;
		packetSize = 150;
		utpBuffer = new Stream(5000);
	}

	/**
	 * Creates a new uTP socket
	 */
	public UtpSocket(boolean utpEnabled) {
		this.utpEnabled = utpEnabled;
		utpConnectionState = ConnectionState.PENDING;
		packetSize = 150;
		utpBuffer = new Stream(5000);
	}

	/**
	 * Connects to the TCP Socket and prepares the UDP Socket
	 * 
	 * @param address The address to connect to
	 */
	public void connect(SocketAddress address) throws IOException {
		connect(address, 30000);
	}

	/**
	 * Connects to the TCP Socket and prepares the UDP Socket
	 * 
	 * @param address The address to connect to
	 * @param timeout The maximum amount of miliseconds this connect attempt may take
	 */
	public void connect(SocketAddress address, int timeout) throws IOException {
		super.connect(address, timeout);
		socket = new DatagramSocket(getPort());
	}
	
	/**
	 * Accept a UDP Packet from the UDP Multiplexer
	 * 
	 * @param dataBuffer The data buffer used in the packet
	 * @param length The amount of bytes received in the packet
	 */
	public void receive(byte[] dataBuffer, int length) {
		if(length < 20) {
			System.err.println("UDP Packet to small for header: " + length + " bytes");
			return;
		}
		Stream data = new Stream(dataBuffer);
		int versionAndType = data.readByte();
		int version = versionAndType & 0x7;
		int type = versionAndType >>> 4;
		System.out.println("Received UDP Message: " + type + " (Version " + version + "), Size: " + length);
		int extension = 0;
		while((extension = data.readByte()) > 0) {
			int extensionLength = data.readShort();
			System.out.println("Extension: " + extension + " (Length: " + extensionLength + ")");
			data.skipWrite(extensionLength);
		}
		int connection_id = data.readShort();
		long timestamp = data.readLong();
		long timestampDiff = data.readLong();
		long windowSize = data.readLong();
		int sequenceNumber = data.readShort();
		int ackNumber = data.readShort();
		System.out.println("Connection ID: " + connection_id);
		System.out.println("timestamp: " + timestamp);
		System.out.println("timestampDiff: " + timestampDiff);
		System.out.println("windowSize: " + windowSize);
		System.out.println("sequenceNumber: " + sequenceNumber);
		System.out.println("ackNumber: " + ackNumber);
	}
	
	/**
	 * Store the filtered data in the output buffer<br/>
	 * This data will be available for the client to read
	 * @param dataBuffer The buffer containing all data
	 * @param offset The start offset to start reading
	 * @param length The amount of bytes to be read
	 */
	private void storeData(byte[] dataBuffer, int offset, int length) {
		if(utpBuffer.writeableSpace() >= length) {
			for(int i = 0; i < length; i++) {
				utpBuffer.writeByte(dataBuffer[offset + i]);
			}
		} else {
			utpBuffer.refit(); //Try to reshape the buffer so we can append the data
			if(utpBuffer.writeableSpace() < length) { //Still not enough so we expand the buffer
				utpBuffer.expand(length - utpBuffer.writeableSpace());
			}
			storeData(dataBuffer, offset, length);
		}
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
			if(socket == null)
				return false; //Else it won't even connect
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
		return new ByteInputStream(this, peer, getInputStream());
	}

	public ByteOutputStream getOutputStream() throws IOException {
		return new ByteOutputStream(this, super.getOutputStream());
	}
	
	/**
	 * Gets the last measured delay on the socket
	 * @return The delay on this socket in microseconds
	 */
	public long getDelay() {
		return measured_delay;
	}
	
	/**
	 * Gets the connection ID which we use to sign messages with
	 * @return
	 */
	public int getConnectionId() {
		return connection_id_send;
	}
	
	/**
	 * Gets the current window size which we want to advertise
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
