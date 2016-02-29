package org.johnnei.javatorrent.tracker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * Tests {@link PeerConnector}
 */
public class PeerConnectorTest extends EasyMockSupport {

	/**
	 * The thread on which the {@link #testStartStop()} methods anonymous class is working.
	 */
	private Thread workerThread;

	@Test
	public void testRun() throws Exception {
		final byte[] peerId = DummyEntity.createPeerId();
		final byte[] torrentHash = DummyEntity.createRandomBytes(20);
		final byte[] extensionBytes = DummyEntity.createRandomBytes(8);

		Torrent torrent = createMock(Torrent.class);
		expect(torrent.getHashArray()).andStubReturn(torrentHash);
		expect(torrent.getFiles()).andReturn(null);
		torrent.addPeer(isA(Peer.class));

		PeerConnectInfo peerConnectInfo = new PeerConnectInfo(torrent, new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		ConnectionDegradation connectionDegradation = createMock(ConnectionDegradation.class);

		TorrentClient torrentClient = createMock(TorrentClient.class);
		expect(torrentClient.getConnectionDegradation()).andReturn(connectionDegradation).atLeastOnce();
		expect(torrentClient.getPeerId()).andReturn(peerId);
		expect(torrentClient.getExtensionBytes()).andReturn(extensionBytes);

		final BitTorrentSocket socket = createMock(BitTorrentSocket.class);
		socket.connect(same(connectionDegradation), same(peerConnectInfo.getAddress()));
		socket.sendHandshake(aryEq(extensionBytes), aryEq(peerId), aryEq(torrentHash));
		expect(socket.readHandshake()).andReturn(new BitTorrentHandshake(torrentHash, extensionBytes, peerId));

		replayAll();

		PeerConnector cut = new DisconnectedPeerConnector(socket, torrentClient);
		cut.enqueuePeer(peerConnectInfo);
		cut.run();

		verifyAll();
	}

	@Test
	public void testRunUnknownTorrent() throws Exception {
		final byte[] peerId = DummyEntity.createPeerId();
		final byte[] torrentHash = DummyEntity.createRandomBytes(20);
		byte[] torrentHashTwo = DummyEntity.createRandomBytes(20);
		while (Arrays.equals(torrentHash, torrentHashTwo)) {
			torrentHashTwo = DummyEntity.createRandomBytes(20);
		}
		final byte[] extensionBytes = DummyEntity.createRandomBytes(8);

		Torrent torrent = createMock(Torrent.class);
		expect(torrent.getHashArray()).andStubReturn(torrentHash);

		PeerConnectInfo peerConnectInfo = new PeerConnectInfo(torrent, new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		ConnectionDegradation connectionDegradation = createMock(ConnectionDegradation.class);

		TorrentClient torrentClient = createMock(TorrentClient.class);
		expect(torrentClient.getConnectionDegradation()).andReturn(connectionDegradation).atLeastOnce();
		expect(torrentClient.getPeerId()).andReturn(peerId);
		expect(torrentClient.getExtensionBytes()).andReturn(extensionBytes);

		final BitTorrentSocket socket = createMock(BitTorrentSocket.class);
		socket.connect(same(connectionDegradation), same(peerConnectInfo.getAddress()));
		socket.sendHandshake(aryEq(extensionBytes), aryEq(peerId), aryEq(torrentHash));
		expect(socket.readHandshake()).andReturn(new BitTorrentHandshake(torrentHashTwo, extensionBytes, peerId));
		socket.close();

		replayAll();

		try {
			PeerConnector cut = new DisconnectedPeerConnector(socket, torrentClient);
			cut.enqueuePeer(peerConnectInfo);
			cut.run();
		} finally {
			verifyAll();
		}
	}

	@Test
	public void testGetConnectingCount() throws Exception {
		final byte[] torrentHash = DummyEntity.createRandomBytes(20);
		byte[] torrentHashTwo = DummyEntity.createRandomBytes(20);

		while (Arrays.equals(torrentHash, torrentHashTwo)) {
			torrentHashTwo = DummyEntity.createRandomBytes(20);
		}

		TorrentClient torrentClient = createMock(TorrentClient.class);
		Torrent torrent = createMock(Torrent.class);
		expect(torrent.getHashArray()).andStubReturn(torrentHash);
		Torrent torrentTwo = createMock(Torrent.class);
		expect(torrentTwo.getHashArray()).andStubReturn(torrentHashTwo);

		PeerConnector cut = new PeerConnector(torrentClient);
		PeerConnectInfo peerConnectInfo = new PeerConnectInfo(torrent, new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		cut.enqueuePeer(null);
		assertEquals("Incorrect connecting count", 0, cut.getConnectingCount());

		cut.enqueuePeer(peerConnectInfo);

		assertEquals("Incorrect connecting count", 1, cut.getConnectingCount());
		assertEquals("Incorrect connecting count", 1, cut.getConnectingCountFor(torrent));
		assertEquals("Incorrect connecting count", 0, cut.getConnectingCountFor(torrentTwo));
	}

	@Test
	public void testRunIOException() throws Exception {
		final byte[] torrentHash = DummyEntity.createRandomBytes(20);

		Torrent torrent = createMock(Torrent.class);
		expect(torrent.getHashArray()).andStubReturn(torrentHash);

		PeerConnectInfo peerConnectInfo = new PeerConnectInfo(torrent, new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		ConnectionDegradation connectionDegradation = createMock(ConnectionDegradation.class);

		TorrentClient torrentClient = createMock(TorrentClient.class);
		expect(torrentClient.getConnectionDegradation()).andReturn(connectionDegradation).atLeastOnce();

		final BitTorrentSocket socket = createMock(BitTorrentSocket.class);
		socket.connect(same(connectionDegradation), same(peerConnectInfo.getAddress()));
		expectLastCall().andThrow(new IOException("Dummy exception!"));
		socket.close();

		replayAll();

		PeerConnector cut = new DisconnectedPeerConnector(socket, torrentClient);
		cut.enqueuePeer(peerConnectInfo);
		cut.run();

		verifyAll();
	}

	@Test
	public void testStartStop() throws Exception {
		final CountDownLatch countDownLatch = new CountDownLatch(1);


		PeerConnector connector = new PeerConnector(null) {
			@Override
			public void run() {
				workerThread = Thread.currentThread();
				countDownLatch.countDown();
			}
		};

		connector.start();

		if (!countDownLatch.await(5, TimeUnit.SECONDS)) {
			fail("Thread did not start");
		}

		connector.stop();
		workerThread.join(5000);
		assertFalse("Thread should have died by now.", workerThread.isAlive());
	}

	private class DisconnectedPeerConnector extends PeerConnector {

		private BitTorrentSocket socket;

		public DisconnectedPeerConnector(BitTorrentSocket socket, TorrentClient torrentClient) {
			super(torrentClient);
			this.socket = socket;
		}

		@Override
		BitTorrentSocket createUnconnectedSocket() {
			// Mock out the socket creation to prevent being testing of BitTorrentSocket methods here.
			return socket;
		}

	}

}