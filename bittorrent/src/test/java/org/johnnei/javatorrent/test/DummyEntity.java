package org.johnnei.javatorrent.test;

import java.util.Random;

import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.network.BitTorrentSocket;

public class DummyEntity {

	public static Peer createPeer(Torrent torrent) {
		return new Peer(new BitTorrentSocket(null), torrent);
	}

	public static byte[] createRandomBytes(int amount) {
		Random random = new Random();

		byte[] bytes = new byte[20];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) (random.nextInt() & 0xff);
		};
		return bytes;
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

}
