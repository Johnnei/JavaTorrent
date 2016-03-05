package org.johnnei.javatorrent.test;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.Random;

import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

public class DummyEntity {

	public static Peer createPeer(Torrent torrent) {
		return new Peer(new BitTorrentSocket(null), torrent, new byte[8]);
	}

	public static byte[] createRandomBytes(int amount) {
		Random random = new Random();

		byte[] bytes = new byte[20];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) (random.nextInt() & 0xff);
		}
		return bytes;
	}

	public static byte[] createUniqueTorrentHash(byte[]... hashes) {
		boolean passed;
		byte[] newHash;

		do {
			passed = true;
			newHash = createRandomBytes(20);
			for (byte[] hash : hashes) {
				if (Arrays.equals(newHash, hash)) {
					passed = false;
					break;
				}
			}

		} while (!passed);

		return newHash;
	}

	public static Torrent createTorrent() {
		return new Torrent.Builder()
				.setHash(createRandomBytes(20))
				.setName("Dummy Torrent")
				.build();
	}

	public static byte[] createPeerId() {
		Random random = new Random();
		byte[] peerId = new byte[20];
		peerId[0] = '-';
		peerId[1] = 'J';
		peerId[2] = 'T';
		peerId[3] = '0';
		peerId[4] = '0';
		peerId[5] = '1';
		peerId[6] = '1';
		peerId[7] = '-';
		for (int i = 8; i < peerId.length; i++) {
			peerId[i] = (byte) (random.nextInt() & 0xFF);
		}
		return peerId;
	}

	public static int findAvailableTcpPort() throws IOException {
		ServerSocket socket = null;
		try {
			socket = new ServerSocket(0);
			return socket.getLocalPort();
		} finally {
			if (socket != null) {
				socket.close();
			}
		}
	}

	public static int findAvailableUdpPort() {
		int port = 27960;
		DatagramSocket socket = null;
		while (port <= 0xFFFF) {
			try {
				socket = new DatagramSocket(port);
				return port;
			} catch (Exception e) {
				// Port not available.
				port++;
			} finally {
				if (socket != null) {
					socket.close();
				}
			}
		}

		throw new IllegalStateException("All ports from 27960 and up are in use.");
	}

}
