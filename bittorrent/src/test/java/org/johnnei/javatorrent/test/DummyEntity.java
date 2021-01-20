package org.johnnei.javatorrent.test;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;
import java.util.function.Supplier;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static java.lang.Integer.min;
import static org.mockito.Mockito.mock;

public class DummyEntity {

	public static byte[] createRandomBytes(int amount) {
		Random random = new Random();

		byte[] bytes = new byte[amount];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) (random.nextInt() & 0xff);
		}
		return bytes;
	}

	public static byte[] createUniqueTorrentHash(byte[]... hashes) {
		return createUniqueArray(() -> createRandomBytes(20), hashes);
	}

	public static byte[] createUniquePeerId(byte[]... peerIds) {
		return createUniqueArray(DummyEntity::createPeerId, peerIds);
	}

	private static byte[] createUniqueArray(Supplier<byte[]> supplier, byte[]... hashes) {
		boolean passed;
		byte[] newHash;

		do {
			passed = true;
			newHash = supplier.get();
			for (byte[] hash : hashes) {
				if (Arrays.equals(newHash, hash)) {
					passed = false;
					break;
				}
			}

		} while (!passed);

		return newHash;
	}

	public static byte[] toPeerId(String idString) {
		return Arrays.copyOf(idString.getBytes(StandardCharsets.UTF_8), 20);
	}

	public static Peer.Builder withPeer() {
		return new Peer.Builder()
			.setSocket(mock(BitTorrentSocket.class))
			.setTorrent(createUniqueTorrent())
			.setExtensionBytes(createRandomBytes(8))
			.setId(createPeerId());
	}

	public static Peer createPeer() {
		return withPeer().build();
	}

	public static Peer createPeer(BitTorrentSocket socket, Torrent torrent) {
		return withPeer()
			.setSocket(socket)
			.setTorrent(torrent)
			.build();
	}

	public static Metadata createMetadata() {
		return new Metadata.Builder().setHash(createUniqueTorrentHash()).build();
	}

	public static Torrent createUniqueTorrent(Torrent... torrents) {
		byte[][] hashes = new byte[torrents.length][];
		for (int i = 0; i < torrents.length; i++) {
			hashes[i] = torrents[i].getMetadata().getHash();
		}

		byte[] hash = createUniqueTorrentHash(hashes);

		return new Torrent.Builder()
				.setMetadata(new Metadata.Builder().setHash(hash).build())
				.setName("Dummy Torrent")
				.build();
	}

	public static Torrent createUniqueTorrent(TorrentClient torrentClient, Torrent... torrents) {
		byte[][] hashes = new byte[torrents.length][];
		for (int i = 0; i < torrents.length; i++) {
			hashes[i] = torrents[i].getMetadata().getHash();
		}

		byte[] hash = createUniqueTorrentHash(hashes);

		return new Torrent.Builder()
				.setMetadata(new Metadata.Builder().setHash(hash).build())
				.setTorrentClient(torrentClient)
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
