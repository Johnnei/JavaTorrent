package org.johnnei.javatorrent.network;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;

import static org.johnnei.javatorrent.test.TestUtils.assertEqualityMethods;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Tests {@link PeerConnectInfo}
 */
public class PeerConnectInfoTest {

	@Test
	public void testEqualsAndHashcode() throws Exception {
		Torrent torrent = DummyEntity.createUniqueTorrent();
		Torrent torrentTwo = DummyEntity.createUniqueTorrent(torrent);

		InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 12);
		InetSocketAddress socketAddressTwo = new InetSocketAddress(InetAddress.getLocalHost(), 13);

		PeerConnectInfo base = new PeerConnectInfo(torrent, socketAddress);
		PeerConnectInfo equalToBase = new PeerConnectInfo(torrent, socketAddress);
		PeerConnectInfo notEqualToBase = new PeerConnectInfo(torrentTwo, socketAddressTwo);
		PeerConnectInfo notEqualToBaseTwo = new PeerConnectInfo(torrent, socketAddressTwo);

		assertEqualityMethods(base, equalToBase, notEqualToBase);
		assertFalse(notEqualToBase.equals(notEqualToBaseTwo), "Equals method should not have matched");
	}

}
