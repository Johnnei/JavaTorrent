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
import torrent.network.Stream;

public class UdpMultiplexer extends Thread implements Logable {
	
	private final Object PACKETLIST_LOCK = new Object();
	private DatagramSocket socket;
	/**
	 * The packets which have not yet been accepted by any of the connections
	 */
	private ArrayList<DatagramPacket> packetList;
	
	public UdpMultiplexer() throws IOException {
		super("UDP Manager");
		packetList = new ArrayList<>();
		socket = new DatagramSocket(Config.getConfig().getInt("download-port", DefaultConfig.DOWNLOAD_PORT));
		socket.setSoTimeout(2500);
	}
	
	@Override
	public void run() {
		log("Initialised UDP Multiplexer");
		while(true) {
			byte[] dataBuffer = new byte[2048];
			DatagramPacket packet = new DatagramPacket(dataBuffer, dataBuffer.length);
			try {
				socket.receive(packet);
				synchronized (PACKETLIST_LOCK) {
					packetList.add(packet);
				}
				log("Received Message of " + packet.getLength() + " bytes for " + packet.getAddress());
			} catch (SocketTimeoutException e) {
				//Ignore
			} catch (IOException e) {
				continue;
			}
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
		for(int packetIndex = 0; packetIndex < packetList.size(); packetIndex++) {
			DatagramPacket packet = packetList.get(packetIndex);
			String packetIp = packet.getSocketAddress().toString().split(":")[0];
			String expectedIp = ip.toString().split(":")[0];
			if(packetIp.equals(expectedIp)) { //If the IP matches we will start deeper checks
				Stream data = new Stream(packet.getData());
				data.skipWrite(packet.getOffset());
				data.readShort(); //Version and Type byte and the extension byte
				int connId = data.readShort();
				if(connId == connectionId || connectionId == UtpSocket.NO_CONNECTION) {
					synchronized (PACKETLIST_LOCK) {
						packetList.remove(packetIndex);
					}
					byte[] rawData = packet.getData();
					byte[] packetData = new byte[packet.getLength()];
					int offset = packet.getOffset();
					for(int i = 0; i < packetData.length; i++) {
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
