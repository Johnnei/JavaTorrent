package org.johnnei.javatorrent.download.tracker.udp;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

public class UdpSocketUtils {

	public UdpSocketUtils() {
	}

	/**
	 * Reads a message from a UDP stream up to 4 KiB
	 *
	 * @param socket
	 * @return
	 * @throws IOException
	 */
	public InStream read(DatagramSocket socket) throws IOException {
		byte[] buffer = new byte[4096];
		DatagramPacket dp = new DatagramPacket(buffer, 0, buffer.length);
		socket.receive(dp);

		return new InStream(buffer, 0, dp.getLength());
	}

	/**
	 * Sends a packet onto a UDP socket
	 *
	 * @param socket
	 * @param address
	 * @param port
	 * @param data
	 * @throws IOException
	 */
	public void write(DatagramSocket socket, InetSocketAddress address, OutStream outStream) throws IOException {
		byte[] data = outStream.toByteArray();
		DatagramPacket packet = new DatagramPacket(data, data.length, address.getAddress(), address.getPort());
		socket.send(packet);
	}

}
