package torrent.network;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.Socket;
import java.net.SocketAddress;

import torrent.download.peer.Peer;

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
	public static final byte STATE_CONNECTING = 0;
	/**
	 * This state will be set as soon as {@link #ST_SYN} has been acked with {@link #ST_STATE}
	 */
	public static final byte STATE_CONNECTED = 1;
	/**
	 * This state will be set on receive of {@link #ST_FIN}
	 */
	public static final byte STATE_DISCONNECTED = 2;

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
	 * The state of the uTP Connection.<br/>
	 * For {@link #ST_RESET} is (as specified) no state
	 */
	private int utpConnectionState;
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
	 * A buffer to store packets in<br/>
	 * Only used for packets which did not get fully received
	 */
	private Stream udpBuffer;

	/**
	 * Creates a new uTP socket
	 */
	public UtpSocket() {
		utpEnabled = false;
		utpConnectionState = STATE_CONNECTING;
		packetSize = 150;
		utpBuffer = new Stream(5000);
		udpBuffer = new Stream(packetSize + 20);
	}

	/**
	 * Gets the current microseconds<br/>
	 * The problem is that java does not have a precise enough system so it's just the currentMillis * 1000<br/>
	 * 
	 * @return
	 */
	private long getCurrentMicroseconds() {
		return System.currentTimeMillis() * 1000;
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
	 * Processes the socket for the available data
	 */
	public void poll() {
		if (utpEnabled) {
			// socket.re
		}
	}

	/**
	 * Switches the stream to uTP<br/>
	 * This will close the TCP Connection
	 * 
	 * @param initiator Set to true if we are the initializing side and therefore we are allowed to define the ID's
	 */
	public void enableUTP(boolean initiator) throws IOException {
		super.close();
		utpEnabled = true;
		if (initiator) {
			// TODO Implement
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
	 * Gets the available data on the stream
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
