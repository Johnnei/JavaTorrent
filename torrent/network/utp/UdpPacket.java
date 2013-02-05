package torrent.network.utp;

import java.net.DatagramPacket;

public class UdpPacket {
	
	private DatagramPacket packet;
	private long receiveTime;
	
	public UdpPacket(DatagramPacket packet) {
		this.packet = packet;
		receiveTime = System.currentTimeMillis();
	}
	
	public long getLifetime() {
		return System.currentTimeMillis() - receiveTime;
	}
	
	public DatagramPacket getPacket() {
		return packet;
	}

}
