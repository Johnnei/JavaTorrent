package torrent.network.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedList;
import java.util.Random;

import org.johnnei.utils.ThreadUtils;

import torrent.network.Stream;
import torrent.network.protocol.utp.ConnectionState;
import torrent.network.protocol.utp.UdpMultiplexer;
import torrent.network.protocol.utp.UtpClient;
import torrent.network.protocol.utp.UtpInputStream;
import torrent.network.protocol.utp.UtpOutputStream;
import torrent.network.protocol.utp.packet.Packet;
import torrent.network.protocol.utp.packet.PacketFin;
import torrent.network.protocol.utp.packet.PacketSample;
import torrent.network.protocol.utp.packet.PacketState;
import torrent.network.protocol.utp.packet.PacketSyn;
import torrent.network.protocol.utp.packet.UtpProtocol;
import torrent.util.tree.BinarySearchTree;

public class UtpSocket implements ISocket, Comparable<UtpSocket> {

	private InetSocketAddress socketAddress;
	private ConnectionState connectionState;
	private UtpClient myClient;
	private UtpClient peerClient;
	/**
	 * The packets which could not be send due to windowSize constraints
	 */
	private LinkedList<Packet> packetQueue;
	/**
	 * The packets which not yet have been acked
	 */
	private BinarySearchTree<Packet> packetsInFlight;
	/**
	 * The total amount of bytes in flight
	 */
	private int bytesInFlight;
	/**
	 * The current sequence number
	 */
	private int sequenceNumber;
	/**
	 * The last received packet
	 */
	private int acknowledgeNumber;
	/**
	 * The timeout which is allowed on this socket for a packet
	 */
	private int timeout;
	/**
	 * The last packet we will receive
	 */
	private int finalAckNumber;
	private UtpInputStream inStream;
	private UtpOutputStream outStream;
	
	/**
	 * Creates a sample socket for comparison
	 * @param connectionId
	 */
	public UtpSocket(short connectionId) {
		peerClient = new UtpClient();
		peerClient.setConnectionId(connectionId);
	}
	
	public UtpSocket() {
		connectionState = ConnectionState.CONNECTING;
		myClient = new UtpClient();
		peerClient = new UtpClient();
		packetQueue = new LinkedList<>();
		packetsInFlight = new BinarySearchTree<>();
		inStream = new UtpInputStream();
		outStream = new UtpOutputStream(this);
		timeout = 1000;
	}
	
	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
		//System.out.println("[uTP] Connecting to " + endpoint);
		this.socketAddress = endpoint;
		peerClient.setConnectionId(new Random().nextInt() & 0xFFFF);
		myClient.setConnectionId(peerClient.getConnectionId() + 1);
		sequenceNumber = 1;
		acknowledgeNumber = 0;
		timeout = 1000;
		PacketSyn connectPacket = new PacketSyn();
		int tries = 0;
		UdpMultiplexer.getInstance().register(this);
		while(tries < 3 && connectionState != ConnectionState.CONNECTED) {
			sendPacket(connectPacket);
			tries++;
			ThreadUtils.sleep(timeout);
		}
		if(connectionState == ConnectionState.CONNECTING) {
			connectionState = ConnectionState.CLOSED;
			UdpMultiplexer.getInstance().unregister(this);
			throw new IOException("Host unreachable");
		}
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return inStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		return outStream;
	}

	@Override
	public void close() throws IOException {
		connectionState = ConnectionState.DISCONNECTING;
		outStream.flush();
		sendPacket(new PacketFin());
	}

	@Override
	public ISocket getFallbackSocket() {
		return new TcpSocket();
	}

	@Override
	public boolean canFallback() {
		return true;
	}

	@Override
	public boolean isClosed() {
		return connectionState == ConnectionState.CLOSED;
	}

	@Override
	public boolean isInputShutdown() {
		return connectionState == ConnectionState.DISCONNECTING || connectionState == ConnectionState.CLOSED;
	}

	@Override
	public boolean isOutputShutdown() {
		return connectionState == ConnectionState.DISCONNECTING || connectionState == ConnectionState.CLOSED;
	}
	
	public UtpClient getMyClient() {
		return myClient;
	}
	
	public UtpClient getPeerClient() {
		return peerClient;
	}

	@Override
	public int compareTo(UtpSocket otherSocket) {
		return peerClient.getConnectionId() - otherSocket.getPeerClient().getConnectionId();
	}
	
	/**
	 * Gets the sequence number and then advances it to the next number
	 * @return
	 */
	public synchronized int getNextSequenceNumber() {
		short seqnr = getSequenceNumber();
		++sequenceNumber;
		return seqnr;
	}

	public short getSequenceNumber() {
		return (short)(sequenceNumber & 0xFFFF);
	}

	public int getAcknowledgeNumber() {
		return acknowledgeNumber;
	}

	public void setSequenceNumber(int sequenceNumber) {
		this.sequenceNumber = sequenceNumber;
	}

	public void setAcknowledgeNumber(int acknowledgeNumber, boolean needAck) {
		if(acknowledgeNumber > this.acknowledgeNumber)
			this.acknowledgeNumber = acknowledgeNumber;
		if(needAck) {
			//System.out.println(myClient.getConnectionId() + "| Ack Send: " + acknowledgeNumber);
			sendPacket(new PacketState(acknowledgeNumber));
		}
	}
	
	public void acknowledgedPacket(int acknowledgeNumber) {
		Packet p = new PacketSample(acknowledgeNumber);
		p = packetsInFlight.remove(p);
		if(p != null) {
			//System.out.println(peerClient.getConnectionId() + "| Acked " + p.getClass().getSimpleName());
			bytesInFlight -= p.getSize();
		}
	}
	
	private void setTimeout(int timeout) {
		this.timeout = Math.max(500, timeout);
	}
	
	/**
	 * Adds this packet to the queue and then tries to send all packets in the queue
	 * @param packet
	 */
	public void sendPacket(Packet packet) {
		packet.setSocket(this);
		packetQueue.addLast(packet);
		sendPacketQueue();
	}
	
	private synchronized void sendPacketQueue() {
		while(!packetQueue.isEmpty()) {
			Packet packet = packetQueue.removeFirst();
			if(packet.getSize() + bytesInFlight < peerClient.getWindowSize()) {
				sendPacketToPeer(packet);
			} else {
				break;
			}
		}
	}
	
	/**
	 * Sends a packet to the peer<br/>
	 * Checks with windows should be applied before calling this function
	 * @param packet
	 */
	private void sendPacketToPeer(Packet packet) {
		Stream outStream = new Stream(packet.getSize());
		packet.write(outStream);
		byte[] dataBuffer = outStream.getBuffer();
		DatagramPacket udpPacket;
		try {
			udpPacket = new DatagramPacket(dataBuffer, dataBuffer.length, socketAddress);
			//System.out.println(myClient.getConnectionId() + "| Send " + packet.getClass().getSimpleName() + " with id: " + packet.getSequenceNumber() + ", their delay: " + peerClient.getDelay());
			if(!UdpMultiplexer.getInstance().send(udpPacket)) {
				throw new SocketException("Failed to send packet");
			}
			if(packet.needAcknowledgement() && packetsInFlight.find(packet) == null) {
				packetsInFlight.add(packet);
				bytesInFlight += dataBuffer.length;
			}
		} catch (SocketException e) {
			packetQueue.addFirst(packet);
		}
	}
	
	public void checkTimeouts() {
		if(connectionState == ConnectionState.CONNECTING)
			return;
		for(Packet packet : packetsInFlight) {
			long currentTime = UtpProtocol.getMicrotime();
			if(currentTime - packet.getSendTime() >= (timeout * 1000)) {
				sendPacketToPeer(packet); //We don't need to check if this would fit in the window, This is already in the window
				setTimeout(timeout * 2);
				//System.out.println(myClient.getConnectionId() + "| " + packet.getClass().getSimpleName() + " (" +packet.getSequenceNumber() + ") timed-out, Timeout increased to " + timeout + "ms");
			}
		}
		sendPacketQueue();
	}

	public void setConnectionState(ConnectionState connectionState) {
		//System.out.println(myClient.getConnectionId() + "| " + connectionState);
		this.connectionState = connectionState;
	}

	public ConnectionState getConnectionState() {
		return connectionState;
	}

	@Override
	public boolean isConnecting() {
		return connectionState == ConnectionState.CONNECTING;
	}
	
	@Override
	public String toString() {
		return socketAddress.toString().substring(1);
	}

	@Override
	public void flush() throws IOException {
		outStream.flush();
	}
	
	public void setFinalPacket(int seqNr) {
		finalAckNumber = seqNr;
	}

	/**
	 * Checks if the Disconnecting phase has been completed
	 */
	public void checkDisconnect() {
		if(connectionState == ConnectionState.DISCONNECTING) {
			if(acknowledgeNumber == finalAckNumber && bytesInFlight == 0) {
				setConnectionState(ConnectionState.CLOSED);
				UdpMultiplexer.getInstance().unregister(this);
			}
		}
	}

	public void setUtpInputNumber(int sequenceNumber) {
		inStream.setSequenceNumber(sequenceNumber);
	}

}
