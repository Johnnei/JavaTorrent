package torrent.network.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.LinkedList;

import torrent.network.Stream;
import torrent.network.protocol.utp.ConnectionState;
import torrent.network.protocol.utp.UdpMultiplexer;
import torrent.network.protocol.utp.UtpClient;
import torrent.network.protocol.utp.UtpInputStream;
import torrent.network.protocol.utp.UtpOutputStream;
import torrent.network.protocol.utp.packet.Packet;
import torrent.network.protocol.utp.packet.PacketSample;
import torrent.network.protocol.utp.packet.PacketState;
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
		outStream = new UtpOutputStream();
	}
	
	@Override
	public void connect(InetSocketAddress endpoint) throws IOException {
		this.socketAddress = endpoint;
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
		//TODO Send PacketFin
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
	public int getNextSequenceNumber() {
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

	public void setAcknowledgeNumber(int acknowledgeNumber) {
		this.acknowledgeNumber = acknowledgeNumber;
		sendPacket(new PacketState());
	}
	
	public void acknowledgedPacket(int acknowledgeNumber) {
		Packet p = new PacketSample(acknowledgeNumber);
		p = packetsInFlight.remove(p);
		if(p != null) {
			bytesInFlight -= p.getSize();
		}
	}
	
	/**
	 * Adds this packet to the queue and then tries to send all packets in the queue
	 * @param packet
	 */
	public void sendPacket(Packet packet) {
		packetQueue.addLast(packet);
		packet = packetQueue.removeFirst();
		//Send all packets which we can
		while(packet.getSize() + bytesInFlight < peerClient.getWindowSize()) {
			Stream outStream = new Stream(packet.getSize());
			packet.write(outStream);
			byte[] dataBuffer = outStream.getBuffer();
			DatagramPacket udpPacket;
			try {
				udpPacket = new DatagramPacket(dataBuffer, dataBuffer.length, socketAddress);
				if(!UdpMultiplexer.getInstance().send(udpPacket)) {
					throw new SocketException("Failed to send packet");
				}
				bytesInFlight += dataBuffer.length;
				packetsInFlight.add(packet);
			} catch (SocketException e) {
				packetQueue.addFirst(packet);
			}
			if(packetQueue.isEmpty()) {
				break;
			} else {
				packet = packetQueue.removeFirst();
			}
		}
	}

	public void setConnectionState(ConnectionState connectionState) {
		this.connectionState = connectionState;
	}

}
