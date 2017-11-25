package org.johnnei.javatorrent.tracker;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PeerConnector}
 */
public class PeerConnectorTest {

	@Rule
	public Timeout timeout = Timeout.seconds(10);

	/**
	 * The thread on which the {@link #testStartStop()} methods anonymous class is working.
	 */
	private Thread workerThread;

	private Torrent torrent;

	private void prepareMetadata(byte[] torrentHash) {
		torrent = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		when(metadataMock.getHash()).thenReturn(torrentHash);
		when(torrent.getMetadata()).thenReturn(metadataMock);
	}

	@Test
	public void testRun() throws Exception {
		final byte[] peerId = DummyEntity.createPeerId();
		final byte[] torrentHash = DummyEntity.createRandomBytes(20);
		final byte[] extensionBytes = DummyEntity.createRandomBytes(8);

		prepareMetadata(torrentHash);

		torrent.addPeer(isA(Peer.class));

		PeerConnectInfo peerConnectInfo = new PeerConnectInfo(torrent, new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		ConnectionDegradation connectionDegradation = mock(ConnectionDegradation.class);
		IPeerDistributor peerDistributor = mock(IPeerDistributor.class);

		TorrentClient torrentClient = mock(TorrentClient.class);
		when(torrentClient.getConnectionDegradation()).thenReturn(connectionDegradation);
		when(torrentClient.getPeerId()).thenReturn(peerId);
		when(torrentClient.getExtensionBytes()).thenReturn(extensionBytes);
		when(torrentClient.getPeerDistributor()).thenReturn(peerDistributor);

		final BitTorrentSocket socket = mock(BitTorrentSocket.class);
		socket.connect(same(connectionDegradation), same(peerConnectInfo.getAddress()));
		socket.sendHandshake(aryEq(extensionBytes), aryEq(peerId), aryEq(torrentHash));
		when(socket.readHandshake()).thenReturn(new BitTorrentHandshake(torrentHash, extensionBytes, peerId));


		PeerConnector cut = new DisconnectedPeerConnector(socket, torrentClient);
		cut.enqueuePeer(peerConnectInfo);
		cut.run();
	}

	@Test
	public void testRunInterrupt() throws Exception {
		BitTorrentSocket socket = mock(BitTorrentSocket.class);
		TorrentClient torrentClient = mock(TorrentClient.class);

		PeerConnector cut = new DisconnectedPeerConnector(socket, torrentClient);

		ReentrantLock lock = (ReentrantLock) Whitebox.getInternalState(cut, Lock.class);
		Condition condition = Whitebox.getInternalState(cut, Condition.class);

		Thread testThread = new Thread(cut, "Test Worker Thread");
		testThread.start();

		await().atMost(1, TimeUnit.SECONDS).until(() -> {
			lock.lock();
			try {
				lock.hasWaiters(condition);
			} finally {
				lock.unlock();
			}
		});

		testThread.interrupt();
		testThread.join(5000);
	}

	@Test
	public void testRunWaitForSignal() throws Exception {
		BitTorrentSocket socket = mock(BitTorrentSocket.class);
		IPeerDistributor peerDistributorMock = mock(IPeerDistributor.class);
		TorrentClient torrentClient = mock(TorrentClient.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		Torrent torrentMock = mock(Torrent.class);
		PeerConnectInfo peerConnectInfo = new PeerConnectInfo(torrentMock, new InetSocketAddress(InetAddress.getLocalHost(), 27960));


		when(peerDistributorMock.hasReachedPeerLimit(any())).thenReturn(true);
		when(torrentClient.getPeerDistributor()).thenReturn(peerDistributorMock);
		when(torrentClient.getExecutorService()).thenReturn(executorServiceMock);

		PeerConnector cut = new DisconnectedPeerConnector(socket, torrentClient);

		ReentrantLock lock = (ReentrantLock) Whitebox.getInternalState(cut, Lock.class);
		Condition condition = Whitebox.getInternalState(cut, Condition.class);

		new Thread(cut, "Test Worker Thread").start();

		await().atMost(1, TimeUnit.SECONDS).until(() -> {
			lock.lock();
			try {
				lock.hasWaiters(condition);
			} finally {
				lock.unlock();
			}
		});

		cut.enqueuePeer(peerConnectInfo);

		await().atMost(3, TimeUnit.SECONDS).until(() -> cut.getConnectingCount() == 0);
	}

	@Test
	public void testRunPeerDistributorDecline() throws Exception {
		BitTorrentSocket socket = mock(BitTorrentSocket.class);
		IPeerDistributor peerDistributorMock = mock(IPeerDistributor.class);
		TorrentClient torrentClient = mock(TorrentClient.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		Torrent torrentMock = mock(Torrent.class);
		PeerConnectInfo peerConnectInfo = new PeerConnectInfo(torrentMock, new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		when(peerDistributorMock.hasReachedPeerLimit(any())).thenReturn(true);
		when(torrentClient.getPeerDistributor()).thenReturn(peerDistributorMock);
		when(torrentClient.getExecutorService()).thenReturn(executorServiceMock);

		PeerConnector cut = new DisconnectedPeerConnector(socket, torrentClient);
		cut.enqueuePeer(peerConnectInfo);
		cut.run();

		ArgumentCaptor<Runnable> runnableCapture = ArgumentCaptor.forClass(Runnable.class);
		verify(executorServiceMock).schedule(runnableCapture.capture(), eq(10L), eq(TimeUnit.SECONDS));

		assertEquals("Peer connector queue must be empty", 0, cut.getConnectingCount());

		// Executing the captured runnable should requeue the peer.
		runnableCapture.getValue().run();
		assertEquals("Peer connector queue must not be empty", 1, cut.getConnectingCount());
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

		prepareMetadata(torrentHash);

		PeerConnectInfo peerConnectInfo = new PeerConnectInfo(torrent, new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		ConnectionDegradation connectionDegradation = mock(ConnectionDegradation.class);
		IPeerDistributor peerDistributor = mock(IPeerDistributor.class);

		TorrentClient torrentClient = mock(TorrentClient.class);
		when(torrentClient.getConnectionDegradation()).thenReturn(connectionDegradation);
		when(torrentClient.getPeerId()).thenReturn(peerId);
		when(torrentClient.getExtensionBytes()).thenReturn(extensionBytes);
		when(torrentClient.getPeerDistributor()).thenReturn(peerDistributor);

		final BitTorrentSocket socket = mock(BitTorrentSocket.class);
		socket.connect(same(connectionDegradation), same(peerConnectInfo.getAddress()));
		socket.sendHandshake(aryEq(extensionBytes), aryEq(peerId), aryEq(torrentHash));
		when(socket.readHandshake()).thenReturn(new BitTorrentHandshake(torrentHashTwo, extensionBytes, peerId));
		socket.close();

		PeerConnector cut = new DisconnectedPeerConnector(socket, torrentClient);
		cut.enqueuePeer(peerConnectInfo);
		cut.run();
	}

	@Test
	public void testGetConnectingCount() throws Exception {
		final byte[] torrentHash = DummyEntity.createRandomBytes(20);
		byte[] torrentHashTwo = DummyEntity.createRandomBytes(20);

		while (Arrays.equals(torrentHash, torrentHashTwo)) {
			torrentHashTwo = DummyEntity.createRandomBytes(20);
		}

		TorrentClient torrentClient = mock(TorrentClient.class);

		prepareMetadata(torrentHash);
		Torrent torrent = this.torrent;

		prepareMetadata(torrentHashTwo);
		Torrent torrentTwo = this.torrent;

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

		prepareMetadata(torrentHash);

		PeerConnectInfo peerConnectInfo = new PeerConnectInfo(torrent, new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		ConnectionDegradation connectionDegradation = mock(ConnectionDegradation.class);
		IPeerDistributor peerDistributor = mock(IPeerDistributor.class);

		TorrentClient torrentClient = mock(TorrentClient.class);
		when(torrentClient.getConnectionDegradation()).thenReturn(connectionDegradation);
		when(torrentClient.getPeerDistributor()).thenReturn(peerDistributor);

		final BitTorrentSocket socket = mock(BitTorrentSocket.class);
		doThrow(new IOException("Dummy exception!")).when(socket).connect(same(connectionDegradation), same(peerConnectInfo.getAddress()));

		PeerConnector cut = new DisconnectedPeerConnector(socket, torrentClient);
		cut.enqueuePeer(peerConnectInfo);
		cut.run();

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