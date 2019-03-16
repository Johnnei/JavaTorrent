package org.johnnei.javatorrent.tracker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.network.connector.BitTorrentHandshakeHandler;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.network.socket.NioTcpSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NioPeerConnectorTest {

	private NioPeerConnector cut;

	private Clock clock;

	private TorrentClient torrentClient;
	private ConnectionDegradation degradation;
	private SocketChannel channelOne;
	private SocketChannel channelTwo;
	private SocketChannel channelThree;
	private ISocket socketTypeOne;
	private ISocket socketTypeTwo;
	private ISocket socketTypeThree;

	private SocketChannel tmpChannel;

	private Torrent torrent;

	@BeforeEach
	public void setUp() throws IOException {
		clock = Clock.systemDefaultZone();
		torrentClient = mock(TorrentClient.class);

		degradation = mock(ConnectionDegradation.class);
		when(torrentClient.getConnectionDegradation()).thenReturn(degradation);

		cut = new NioPeerConnector(clock, torrentClient, 4);

		channelOne = SocketChannel.open();
		channelOne.configureBlocking(false);
		channelTwo = SocketChannel.open();
		channelTwo.configureBlocking(false);
		channelThree = SocketChannel.open();
		channelThree.configureBlocking(false);

		socketTypeOne = mock(ISocket.class, "ISocket Type A");
		socketTypeTwo = mock(ISocket.class, "ISocket Type B");
		socketTypeThree = mock(ISocket.class, "ISocket Type C");

		when(socketTypeOne.getReadableChannel()).thenReturn(channelOne);
		when(socketTypeTwo.getReadableChannel()).thenReturn(channelTwo);
		when(socketTypeThree.getReadableChannel()).thenReturn(channelThree);

		when(degradation.createPreferredSocket()).thenReturn(socketTypeOne);
		when(degradation.degradeSocket(same(socketTypeOne))).thenReturn(Optional.of(socketTypeTwo));
		when(degradation.degradeSocket(same(socketTypeTwo))).thenReturn(Optional.empty());
		when(degradation.degradeSocket(same(socketTypeThree))).thenReturn(Optional.empty());

		torrent = mock(Torrent.class);
		Metadata metadata = mock(Metadata.class);
		when(torrent.getMetadata()).thenReturn(metadata);
		when(metadata.getHashString()).thenReturn("A");
	}

	@AfterEach
	public void tearDown() throws Exception {
		channelOne.close();
		channelTwo.close();
		channelThree.close();
	}

	@Test
	public void testPollReadyConnections() throws Exception {
		ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);
		BitTorrentHandshakeHandler handshakeHandler = mock(BitTorrentHandshakeHandler.class);
		Torrent torrent = DummyEntity.createUniqueTorrent();

		cut = new NioPeerConnector(torrentClient, 4);

		when(torrentClient.getExecutorService()).thenReturn(executor);
		when(torrentClient.getHandshakeHandler()).thenReturn(handshakeHandler);

		try (NioTcpSocket socket = new NioTcpSocket(); ServerSocketChannel serverSocket = ServerSocketChannel.open()) {
			when(degradation.createPreferredSocket()).thenReturn(socket);
			serverSocket.configureBlocking(false);
			cut.start();

			serverSocket.bind(null);
			PeerConnectInfo info = new PeerConnectInfo(torrent, (InetSocketAddress) serverSocket.getLocalAddress());

			cut.enqueuePeer(info);

			await("The connection to be established.").atMost(10, TimeUnit.SECONDS).until(() -> {
				try {
					SocketChannel channel = serverSocket.accept();
					if (channel == null) {
						throw new AssertionError("Expected a connection to be established");
					}
					tmpChannel = channel;
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			await("Connection to be handed over to the handshake handler")
				.atMost(10, TimeUnit.SECONDS)
				.until(() -> verify(handshakeHandler).onConnectionEstablished(notNull(), eq(torrent.getMetadata().getHash())));
		} finally {
			if (tmpChannel != null) {
				tmpChannel.close();
			}

			cut.stop();
			executor.shutdown();
		}
	}

	@Test
	public void testDegradeConnection() throws Exception {
		TestClock clock = new TestClock(Clock.systemDefaultZone());
		cut = new NioPeerConnector(clock, torrentClient, 4);

		BitTorrentHandshakeHandler handshakeHandler = mock(BitTorrentHandshakeHandler.class);
		when(torrentClient.getHandshakeHandler()).thenReturn(handshakeHandler);

		PeerConnectInfo infoOne = mock(PeerConnectInfo.class);
		when(infoOne.getAddress())
			.thenReturn(InetSocketAddress.createUnresolved("localhost", DummyEntity.findAvailableTcpPort()));
		when(infoOne.getTorrent()).thenReturn(torrent);

		cut.enqueuePeer(infoOne);
		cut.pollReadyConnections();

		verify(socketTypeOne).connect(notNull());

		clock.setClock(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(15)));

		cut.pollReadyConnections();

		verify(socketTypeOne).close();
		verify(socketTypeTwo).connect(notNull());
	}

	@Test
	public void testEnqueuePeerNull() {
		cut.enqueuePeer(null);
		cut.pollReadyConnections();
		assertThat(cut.getConnectingCount(), is(0));
	}

	@Test
	public void testAdhereConcurrentLimit() {
		PeerConnectInfo infoOne = mock(PeerConnectInfo.class);
		PeerConnectInfo infoTwo = mock(PeerConnectInfo.class);
		PeerConnectInfo infoThree = mock(PeerConnectInfo.class);

		when(infoOne.getTorrent()).thenReturn(torrent);
		when(infoTwo.getTorrent()).thenReturn(torrent);
		when(infoThree.getTorrent()).thenReturn(torrent);

		when(degradation.createPreferredSocket()).thenReturn(socketTypeOne, socketTypeTwo, socketTypeThree);
		cut = new NioPeerConnector(clock, torrentClient, 2);
		cut.enqueuePeer(infoOne);
		cut.enqueuePeer(infoTwo);
		cut.enqueuePeer(infoThree);

		cut.pollReadyConnections();
		assertThat(cut.getConnectingCount(), is(2));
	}

	@Test
	public void testGetConnectingCount() {
		PeerConnectInfo infoOne = mock(PeerConnectInfo.class);
		PeerConnectInfo infoTwo = mock(PeerConnectInfo.class);

		when(infoOne.getTorrent()).thenReturn(torrent);
		when(infoTwo.getTorrent()).thenReturn(torrent);

		cut = new NioPeerConnector(torrentClient, 4);

		when(degradation.createPreferredSocket()).thenReturn(socketTypeOne).thenReturn(socketTypeTwo);

		cut.enqueuePeer(infoOne);
		cut.enqueuePeer(infoTwo);
		cut.pollReadyConnections();
		assertThat(cut.getConnectingCount(), is(2));
	}

	@Test
	public void tesGetConnectingCountFor() {
		Torrent torrent = DummyEntity.createUniqueTorrent();
		Torrent torrentTwo = DummyEntity.createUniqueTorrent(torrent);

		PeerConnectInfo infoOne = mock(PeerConnectInfo.class);
		PeerConnectInfo infoTwo = mock(PeerConnectInfo.class);
		PeerConnectInfo infoThree = mock(PeerConnectInfo.class);

		when(infoOne.getTorrent()).thenReturn(torrent);
		when(infoTwo.getTorrent()).thenReturn(torrentTwo);
		when(infoThree.getTorrent()).thenReturn(torrentTwo);

		cut = new NioPeerConnector(torrentClient, 4);

		when(degradation.createPreferredSocket()).thenReturn(socketTypeOne).thenReturn(socketTypeTwo).thenReturn(socketTypeThree);

		cut.enqueuePeer(infoOne);
		cut.enqueuePeer(infoTwo);
		cut.enqueuePeer(infoThree);
		cut.pollReadyConnections();
		assertThat(cut.getConnectingCount(), is(3));
		assertThat(cut.getConnectingCountFor(torrent), is(1));
		assertThat(cut.getConnectingCountFor(torrentTwo), is(2));
	}
}
