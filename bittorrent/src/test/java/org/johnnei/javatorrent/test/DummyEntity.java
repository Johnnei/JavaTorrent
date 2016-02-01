package org.johnnei.javatorrent.test;

import java.util.Random;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.Peer;
import org.johnnei.javatorrent.torrent.network.BitTorrentSocket;

public class DummyEntity {

	public static Peer createPeer(Torrent torrent) {
		return new Peer(new BitTorrentSocket(null), torrent);
	}

	public static Torrent createTorrent(TorrentClient torrentClient) {
		Random random = new Random();

		byte[] hash = new byte[20];
		for (int i = 0; i < hash.length; i++) {
			hash[i] = (byte) (random.nextInt() & 0xff);
		};

		return new Torrent(torrentClient, hash, "Dummy Torrent");
	}

}
