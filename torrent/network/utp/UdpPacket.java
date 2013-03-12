package torrent.network.utp;

import java.net.DatagramPacket;

public class UdpPacket {
	
	private DatagramPacket packet;
	private long receiveTime;
	private boolean processed;
	
	public UdpPacket(DatagramPacket packet) {
		this.packet = packet;
		receiveTime = System.currentTimeMillis();
		processed = false;
	}
	
	public long getLifetime() {
		return System.currentTimeMillis() - receiveTime;
	}
	
	public DatagramPacket getPacket() {
		return packet;
	}
	
	public void processed() {
		processed = true;
	}
	
	public boolean isProcessed() {
		return processed;
	}

}
