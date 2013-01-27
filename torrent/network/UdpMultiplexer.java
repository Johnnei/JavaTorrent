package torrent.network;

import java.io.IOException;
import java.net.DatagramSocket;

public class UdpMultiplexer extends Thread {
	
	private DatagramSocket socket;
	
	public UdpMultiplexer() {
		try {
			socket = new DatagramSocket(27960);
		} catch (IOException e) {}
	}
	
	@Override
	public void run() {
		
	}

}
