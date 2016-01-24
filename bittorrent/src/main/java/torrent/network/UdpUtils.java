package torrent.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public final class UdpUtils {
	
	/**
	 * Reads a message from a UDP stream up to 4 KiB
	 * @param socket
	 * @return
	 * @throws IOException
	 */
	public final static InStream read(DatagramSocket socket) throws IOException {
		return read(socket, 4096);
	}
	
	/**
	 * Reads message from a UDP stream with the given upper limit of size
	 * @param socket
	 * @param messageSize
	 * @return
	 * @throws IOException
	 */
	public final static InStream read(DatagramSocket socket, int messageSize) throws IOException {
		byte[] buffer = new byte[messageSize];
		DatagramPacket dp = new DatagramPacket(buffer, 0, messageSize);
		socket.receive(dp);
		
		return new InStream(buffer, 0, dp.getLength());
	}
	
	/**
	 * Sends a packet onto a UDP socket
	 * @param socket
	 * @param address
	 * @param port
	 * @param data
	 * @throws IOException
	 */
	public final static void write(DatagramSocket socket, InetAddress address, int port, OutStream data) throws IOException {
		write(socket, address, port, data.toByteArray());
	}
	
	/**
	 * Sends a packet onto a UDP socket
	 * @param socket
	 * @param address
	 * @param port
	 * @param data
	 * @throws IOException
	 */
	public final static void write(DatagramSocket socket, InetAddress address, int port, byte[] data) throws IOException {
		DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
		socket.send(packet);
	}

}
