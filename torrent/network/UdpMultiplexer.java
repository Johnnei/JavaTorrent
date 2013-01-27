package torrent.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;

import torrent.Manager;
import torrent.download.Torrent;
import torrent.download.peer.Peer;

public class UdpMultiplexer extends Thread {
	
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
		while(true) {
			byte[] dataBuffer = new byte[1024];
			DatagramPacket packet = new DatagramPacket(dataBuffer, dataBuffer.length);
			try {
				socket.receive(packet);
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

}
