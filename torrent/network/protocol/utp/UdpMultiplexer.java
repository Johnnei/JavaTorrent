package torrent.network.protocol.utp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Iterator;

import org.johnnei.utils.config.Config;

import torrent.Logable;
import torrent.network.Stream;
import torrent.network.protocol.UtpSocket;
import torrent.network.protocol.utp.packet.Packet;
import torrent.util.tree.BinarySearchTree;

public class UdpMultiplexer extends Thread implements Logable {

	private static UdpMultiplexer instance = new UdpMultiplexer();
	
	public static UdpMultiplexer getInstance() {
		return instance;
	}
	
	private final Object BST_LOCK = new Object();
	/**
	 * The Factory to create the packet instances<br/>
	 * If JavaTorrent will need to update the protocol then we can use multiple factory's to create the correct version of the packet
	 */
	private UtpPacketFactory packetFactory;
	/**
	 * The socket on which the udp packet will be received and send
	 */
	private DatagramSocket multiplexerSocket;
	/**
	 * All {@link UtpSocket}s which have registered to listen for packets
	 */
	private BinarySearchTree<UtpSocket> utpSockets;
	
	private UdpMultiplexer() {
		super("UdpMultiplexer");
		utpSockets = new BinarySearchTree<>();
		packetFactory = new UtpPacketFactory();
		try {
			multiplexerSocket = new DatagramSocket(Config.getConfig().getInt("download-port"));
			new UtpSocketTimeout().start();
			start();
		} catch (IOException e) {
			e.printStackTrace();
		}		
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
	
	public void updateTimeout() {
		Iterator<UtpSocket> i;
		synchronized (BST_LOCK) {
			 i = utpSockets.iterator();
		}
		while(i.hasNext()) {
			UtpSocket socket = i.next();
			socket.checkTimeouts();
			socket.checkDisconnect();
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
			e.printStackTrace();
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
						Stream inStream = new Stream(packet.getData(), packet.getOffset(), packet.getLength());
						utpPacket.read(inStream);
						UtpSocket socket = new UtpSocket(utpPacket.getConnectionId());
						socket = utpSockets.find(socket);
						socket.updateLastInteraction();
						if(socket != null) {
							//log("Received " + utpPacket.getClass().getSimpleName() + " for " + (utpPacket.getConnectionId() & 0xFFFF));
							utpPacket.process(socket);
						} else {
							//log("Packet of " + packet.getLength() + " bytes (0x" + Integer.toHexString(dataBuffer[0]) + ") was send to a connection which was not established (" + packet.getAddress() + ":" + packet.getPort() + " | " + utpPacket.getConnectionId() + ")");
						}
					} catch (IllegalArgumentException e) {
						log("Invalid Packet of " + packet.getLength() + " bytes with type " + type + " (" + packet.getAddress() + ":" + packet.getPort() + ")", true);
					}
				} else {
					log("Invalid Packet of " + packet.getLength() + " bytes with version " + version + " (" + packet.getAddress() + ":" + packet.getPort() + ")", true);
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
		s = "[UdpMultiplexer] " + s;
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
