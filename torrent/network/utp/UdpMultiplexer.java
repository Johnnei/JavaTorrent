package torrent.network.utp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import org.johnnei.utils.config.Config;
import org.johnnei.utils.config.DefaultConfig;

import torrent.Logable;
import torrent.download.peer.Peer;
import torrent.network.Stream;

public class UdpMultiplexer extends Thread implements Logable {

	private final Object PACKETLIST_LOCK = new Object();
	private final Object SEND_LOCK = new Object();
	private UdpPeerConnector peerConnector;
	private DatagramSocket socket;
	/**
	 * The packets which have not yet been accepted by any of the connections
	 */
	private ArrayList<UdpPacket> packetList;

	public UdpMultiplexer() throws IOException {
		super("UDP Manager");
		peerConnector = new UdpPeerConnector();
		peerConnector.start();
		packetList = new ArrayList<>();
		socket = new DatagramSocket(Config.getConfig().getInt("download-port", DefaultConfig.DOWNLOAD_PORT));
		socket.setSoTimeout(2500);
	}
	
	/**
	 * Checks if the received data does match to a packet which might be uTP
	 * @param data The data to check
	 * @return true if the packet type >= 0 && type < 5 && version == 1
	 */
	private boolean isValid(byte[] data) {
		int version = data[0] & 0x7;
		int type = data[0] >>> 4; 
		if(version != 1) {
			return false;
		} else if (type < 0 || type > 4) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public void run() {
		log("Initialised UDP Multiplexer");
		while (true) {
			byte[] dataBuffer = new byte[2048];
			DatagramPacket packet = new DatagramPacket(dataBuffer, dataBuffer.length);
			try {
				socket.receive(packet);
				if(isValid(dataBuffer)) {
					synchronized (PACKETLIST_LOCK) {
						packetList.add(new UdpPacket(packet));
					}
					log("Received Message of " + packet.getLength() + " bytes for " + packet.getAddress());
				} else {
					log("Received Invalid Message of " + packet.getLength() + " bytes for " + packet.getAddress() + ", Type/Version: 0x" + Integer.toHexString(dataBuffer[0]));
				}
			} catch (SocketTimeoutException e) {
				// Ignore
			} catch (IOException e) {
				continue;
			}
			for (int i = 0; i < packetList.size(); i++) {
				UdpPacket udpPacket = packetList.get(i);
				long lifetime = udpPacket.getLifetime();
				if (lifetime > 5000) { // 5 Seconds, The system should be able to loop through all peers per torrent within 5 seconds
					byte[] data = udpPacket.getPacket().getData();
					if (data[udpPacket.getPacket().getOffset()] >>> 4 == UtpSocket.ST_SYN) {
						try {
							log("Unhandled uTP Connection Detected from " + udpPacket.getPacket().getSocketAddress());
							UtpSocket socket = new UtpSocket(udpPacket.getPacket().getSocketAddress(), true);
							Peer p = new Peer();
							p.setSocket(socket);
							synchronized (PACKETLIST_LOCK) {
								packetList.remove(i--);
								if(peerConnector.addPeer(p)) {
									packetList.add(new UdpPacket(udpPacket.getPacket()));
								}
							}
						} catch (IOException e) {
							log(e.getMessage(), true);
						}
					} else {
						synchronized (PACKETLIST_LOCK) {
							packetList.remove(i--);
						}
						log("Dropped Message of " + udpPacket.getPacket().getLength() + " bytes for " + udpPacket.getPacket().getAddress());
					}
				}
			}
		}
	}
	
	public void send(DatagramPacket udpPacket) throws IOException {
		synchronized (SEND_LOCK) {
			socket.send(udpPacket);
		}
	}

	/**
	 * Accepts a packet from the list which matches the ip and port
	 * 
	 * @param ip The Address on which we expect the packet to be send from (I don't check ports as we don't know it)
	 * @param connectionId The connection Id in the packet header
	 * @return the bytes from the packet or null is none is available
	 */
	public byte[] accept(SocketAddress ip, int connectionId) {
		for (int packetIndex = 0; packetIndex < packetList.size(); packetIndex++) {
			DatagramPacket packet = packetList.get(packetIndex).getPacket();
			String packetIp = packet.getSocketAddress().toString().split(":")[0];
			String expectedIp = ip.toString().split(":")[0];
			if (packetIp.equals(expectedIp)) { // If the IP matches we will start deeper checks
				Stream data = new Stream(packet.getData());
				data.skipWrite(packet.getOffset());
				int type = data.readByte() >>> 4;
				data.readByte(); // Version and Type byte and the extension byte
				int connId = data.readShort();
				if(type == UtpSocket.ST_SYN) {
					connId++;
				}
				log(packetIp + " " + connectionId + " =?= " + connId);
				if (connId == connectionId || connectionId == UtpSocket.NO_CONNECTION) {
					synchronized (PACKETLIST_LOCK) {
						packetList.remove(packetIndex);
					}
					byte[] rawData = packet.getData();
					byte[] packetData = new byte[packet.getLength()];
					int offset = packet.getOffset();
					for (int i = 0; i < packetData.length; i++) {
						packetData[i] = rawData[offset + i];
					}
					return packetData;
				}
			}
		}
		return null;
	}

	@Override
	public void log(String s) {
		log(s, false);
	}

	@Override
	public void log(String s, boolean isError) {
		s = "[" + toString() + "] " + s;
		if (isError)
			System.err.println(s);
		else
			System.out.println(s);
	}

	@Override
	public String getStatus() {
		return "";
	}

	@Override
	public String toString() {
		return "UDPMultiplexer";
	}

}
