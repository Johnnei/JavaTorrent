package torrent.network.utp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import torrent.Logable;
import torrent.Manager;
import torrent.download.Torrent;
import torrent.download.peer.Peer;

public class UdpMultiplexer extends Thread implements Logable {
	
	private DatagramSocket socket;
	
	public UdpMultiplexer() {
		super("UDP Manager");
		try {
			socket = new DatagramSocket(27960);
			socket.setSoTimeout(2500);
		} catch (IOException e) {}
	}
	
	@Override
	public void run() {
		log("Initialised UDP Multiplexer");
		while(true) {
			byte[] dataBuffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(dataBuffer, dataBuffer.length);
			try {
				socket.receive(packet);
				log("Received Message of length: " + packet.getLength());
			} catch (SocketTimeoutException e) {
				//Ignore
			} catch (IOException e) {
				continue;
			}
			ArrayList<Torrent> torrents = Manager.getManager().getTorrents();
			for(int i = 0; i < torrents.size(); i++) {
				Torrent torrent = torrents.get(i);
				ArrayList<Peer> peers = torrent.getPeers();
				for(int j = 0; j < peers.size(); j++) {
					UtpSocket utpSocket = peers.get(i).getSocket();
					if(utpSocket.getPort() == packet.getPort()) {
						if(utpSocket.getInetAddress().equals(packet.getAddress())) {
							utpSocket.receive(dataBuffer, packet.getLength());
						}
					}
				}
			}
		}
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
