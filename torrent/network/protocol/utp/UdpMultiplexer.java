package torrent.network.protocol.utp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import torrent.Logable;
import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;
import torrent.network.protocol.utp.packet.Packet;
import torrent.util.tree.BinarySearchTree;

public class UdpMultiplexer extends Thread implements Logable {

	private static UdpMultiplexer instance;
	
	public static UdpMultiplexer getInstance() {
		if(instance == null) {
			instance = new UdpMultiplexer();
			instance.start();
		}
		return instance;
	}
	
	private final Object BST_LOCK = new Object();
	
	private UtpPacketFactory packetFactory;
	private DatagramSocket multiplexerSocket;
	private BinarySearchTree<UtpSocket> utpSockets;
	
	private UdpMultiplexer() {
		try {
			multiplexerSocket = new DatagramSocket(27960);
		} catch (IOException e) {
			e.printStackTrace();
		}
		utpSockets = new BinarySearchTree<>();
		packetFactory = new UtpPacketFactory();
	}
	
	/**
	 * Registers a socket to the UdpMultiplexer<br/>
	 * Packet received will only be directed to registered sockets
	 * @param socket The socket to register
	 */
	public void register(UtpSocket socket) {
		synchronized (BST_LOCK) {
			utpSockets.add(socket);
		}
	}
	
	/**
	 * Removes a socket from the UdpMultiplexer
	 * @param socket The socket to remove
	 */
	public void unregister(UtpSocket socket) {
		synchronized (BST_LOCK) {
			utpSockets.remove(socket);
		}
	}
	
	/**
	 * Tries to send the UdpPacket
	 * @param packet The packet to send
	 * @return If the send call did not throw an exception
	 */
	public synchronized boolean send(DatagramPacket packet) {
		try {
			multiplexerSocket.send(packet);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	@Override
	public void run() {
		while(true) {
			try {
				byte[] dataBuffer = new byte[25600]; //25kB buffer
				DatagramPacket packet = new DatagramPacket(dataBuffer, dataBuffer.length);
				multiplexerSocket.receive(packet);
				//Check if packet is valid
				int type = dataBuffer[0] >>> 4;
				int version = dataBuffer[0] & 0x7;
				if(version == Packet.VERSION) {
					try {
						Packet utpPacket = packetFactory.getFromId(type);
						Stream inStream = new Stream(packet.getLength());
						inStream.writeByte(dataBuffer, 0, packet.getLength());
						utpPacket.read(inStream);
						UtpSocket socket = new UtpSocket(utpPacket.getConnectionId());
						socket = utpSockets.find(socket);
						if(socket != null) {
							utpPacket.process(socket);
						} else {
							log("Packet of " + packet.getLength() + " bytes was send to a connection which was not established");
						}
					} catch (IllegalArgumentException e) {
						log("Invalid Packet of " + packet.getLength() + " bytes with type " + type, true);
					}
				} else {
					log("Invalid Packet of " + packet.getLength() + " bytes with version " + version, true);
				}
			} catch (IOException e) {
				
			}
		}
	}

	@Override
	public void log(String s) {
		log(s, false);
	}

	@Override
	public void log(String s, boolean isError) {
		s = "[UdpMultiplexer] ";
		if(isError) {
			System.err.println(s);
		} else {
			System.out.println(s);
		}
	}

	@Override
	public String getStatus() {
		// TODO Auto-generated method stub
		return null;
	}

}
