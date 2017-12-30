package org.johnnei.javatorrent.internal.network.connector;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.network.PeerIoHandler;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.network.socket.NioTcpSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.Torrent;

import static com.jayway.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.johnnei.javatorrent.network.ByteBufferUtils.getBytes;
import static org.johnnei.javatorrent.network.ByteBufferUtils.getString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BitTorrentHandshakeHandlerImplTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(BitTorrentHandshakeHandlerImplTest.class);

	private BitTorrentHandshakeHandlerImpl cut;

	private ScheduledExecutorService executorService;
	private TorrentClient torrentClient;
	private PeerIoHandler peerIoHandler;
	private byte[] extensionBytes;
	private byte[] peerId;

	@BeforeEach
	public void setUp() {
		torrentClient = mock(TorrentClient.class);
		peerIoHandler = mock(PeerIoHandler.class);
		extensionBytes = DummyEntity.createRandomBytes(8);
		peerId = DummyEntity.createPeerId();
		executorService = new ScheduledThreadPoolExecutor(2);

		when(torrentClient.getPeerId()).thenReturn(peerId);
		when(torrentClient.getExtensionBytes()).thenReturn(extensionBytes);

		when(torrentClient.getExecutorService()).thenReturn(executorService);
		cut = new BitTorrentHandshakeHandlerImpl(torrentClient, peerIoHandler);
	}

	@AfterEach
	public void tearDown() {
		cut.stop();
		executorService.shutdown();
	}

	@Test
	void testOnConnectionEstablished() throws Exception {
		SelectableByteChannel channel = mock(SelectableByteChannel.class);
		ISocket socket = mock(ISocket.class);
		when(socket.getChannel()).thenReturn(channel);

		byte[] expectedHandshake = new byte[] {
			// Protocol Identification
			0x13,
			// B   i     t     T     o     r     r     e    n      t    (space)
			0x42, 0x69, 0x74, 0x54, 0x6F, 0x72, 0x72, 0x65, 0x6E, 0x74, 0x20,
			// p   r     o     t     o     c     o     l
			0x70, 0x72, 0x6F, 0x74, 0x6F, 0x63, 0x6F, 0x6C,
			// Extension Bytes
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			// Torrent Hash
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			// Peer ID
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		};

		byte[] torrentHash = DummyEntity.createUniqueTorrentHash();

		TestUtils.copySection(extensionBytes, expectedHandshake, 20);
		TestUtils.copySection(torrentHash, expectedHandshake, 28);
		TestUtils.copySection(peerId, expectedHandshake, 48);

		when(channel.write(any())).then(inv -> {
			ByteBuffer buffer = inv.getArgument(0);
			buffer.position(buffer.limit());
			return buffer.limit();
		});

		cut.onConnectionEstablished(socket, torrentHash);

		ArgumentCaptor<ByteBuffer> sendBuffer = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(channel).write(sendBuffer.capture());

		ByteBuffer buffer = sendBuffer.getValue().asReadOnlyBuffer();
		buffer.flip();
		assertHandshake(torrentHash, buffer);
	}

	private void assertHandshake(byte[] torrentHash, ByteBuffer buffer) {
		int receivedLength = buffer.get();
		String receivedProtocol = getString(buffer, 0x13);
		byte[] receivedExtensionBytes = getBytes(buffer, 8);
		byte[] receivedHash = getBytes(buffer, 20);
		byte[] receivedPeerId = getBytes(buffer, 20);

		Assertions.assertAll(
			() -> assertThat("Protocol length is 0x13", receivedLength, is(0x13)),
			() -> assertThat("Incorrect protocol name.", receivedProtocol, equalTo("BitTorrent protocol")),
			() -> assertThat("Incorrect extension bytes", receivedExtensionBytes, equalTo(extensionBytes)),
			() -> assertThat("Incorrect hash", receivedHash, equalTo(torrentHash)),
			() -> assertThat("Incorrect peer id", receivedPeerId, equalTo(peerId))
		);
	}

	@Test
	void onConnectionReceived() throws Exception {
		SelectableByteChannel channel = mock(SelectableByteChannel.class);
		ISocket socket = mock(ISocket.class);
		when(socket.getChannel()).thenReturn(channel);

		cut.onConnectionReceived(socket);
		verify(channel).register(any(), eq(SelectionKey.OP_READ), any());
	}

	@ParameterizedTest
	@MethodSource("connectToRemoteScenarios")
	public void testOnConnectionEstablishedFlow(Torrent torrent, byte[] requestedHash, byte[] response, boolean successful) throws IOException {
		when(torrentClient.getTorrentByHash(torrent.getMetadata().getHash())).thenReturn(Optional.of(torrent));

		try (ServerSocketChannel serverChannel = ServerSocketChannel.open(); SocketChannel localChannel = SocketChannel.open()) {
			LOGGER.info("Connecting socket.");
			serverChannel.bind(null);

			localChannel.connect(serverChannel.getLocalAddress());
			localChannel.configureBlocking(false);

			SocketChannel remoteChannel = serverChannel.accept();

			LOGGER.info("Sending handshake to remote.");

			cut.onConnectionEstablished(new NioTcpSocket(localChannel), requestedHash);

			ByteBuffer receivedHandshake = ByteBuffer.allocate(68);
			remoteChannel.read(receivedHandshake);

			LOGGER.info("Validating send handshake.");
			receivedHandshake.flip();
			assertHandshake(requestedHash, receivedHandshake);

			assertThat(torrent.getPeers(), empty());

			LOGGER.info("Sending handshake response.");

			ByteBuffer sendHandshake = ByteBuffer.allocate(68);
			sendHandshake.put(response);
			sendHandshake.flip();
			remoteChannel.write(sendHandshake);

			if (successful) {
				LOGGER.info("Waiting for peer to be added to Torrent.");
				await().until(() -> !torrent.getPeers().isEmpty());
				verify(peerIoHandler).registerPeer(notNull(), notNull());
			} else {
				await().pollDelay(150, TimeUnit.MILLISECONDS).until(() -> torrent.getPeers().isEmpty());
			}
		}

	}

	@ParameterizedTest
	@MethodSource("connectionFromRemoteScenarios")
	public void testOnConnectionReceivedFlow(Torrent torrent, byte[] response, boolean successful) throws IOException {
		when(torrentClient.getTorrentByHash(torrent.getMetadata().getHash())).thenReturn(Optional.of(torrent));

		try (ServerSocketChannel serverChannel = ServerSocketChannel.open(); SocketChannel localChannel = SocketChannel.open()) {
			LOGGER.info("Connecting socket.");
			serverChannel.bind(null);

			localChannel.connect(serverChannel.getLocalAddress());
			localChannel.configureBlocking(false);

			SocketChannel remoteChannel = serverChannel.accept();

			LOGGER.info("Simulating handshake from remote.");

			cut.onConnectionReceived(new NioTcpSocket(localChannel));

			ByteBuffer remoteHandshake = ByteBuffer.allocate(68);
			remoteHandshake.put(response);
			remoteHandshake.flip();
			remoteChannel.write(remoteHandshake);

			if (successful) {
				LOGGER.info("Waiting for handshake response.");
				ByteBuffer sendHandshake = ByteBuffer.allocate(68);
				remoteChannel.read(sendHandshake);
				sendHandshake.flip();

				assertHandshake(torrent.getMetadata().getHash(), sendHandshake);

				LOGGER.info("Waiting for peer to be added to Torrent.");
				await().until(() -> !torrent.getPeers().isEmpty());
				verify(peerIoHandler).registerPeer(notNull(), notNull());
			} else {
				await().pollDelay(150, TimeUnit.MILLISECONDS).until(() -> torrent.getPeers().isEmpty());
			}
		}

	}

	public static Stream<Arguments> connectToRemoteScenarios() {
		TorrentClient torrentClient = mock(TorrentClient.class);
		when(torrentClient.getModules()).thenReturn(Collections.emptyList());

		Torrent torrentPass = DummyEntity.createUniqueTorrent(torrentClient);
		Torrent torrent = DummyEntity.createUniqueTorrent(torrentClient);
		Torrent torrentTwo = DummyEntity.createUniqueTorrent(torrentClient, torrent);

		byte[] incorrectProtocolName = createHandshake(torrent.getMetadata().getHash());
		incorrectProtocolName[12] = 'P';

		byte[] incorrectProtocolLength = createHandshake(torrent.getMetadata().getHash());
		incorrectProtocolLength[0] = 0x12;

		return Stream.of(
			Arguments.of(torrentPass, torrentPass.getMetadata().getHash(), createHandshake(torrentPass.getMetadata().getHash()), true),
			Arguments.of(torrent, torrent.getMetadata().getHash(), createHandshake(torrentTwo.getMetadata().getHash()), false),
			Arguments.of(torrent, torrentTwo.getMetadata().getHash(), createHandshake(torrent.getMetadata().getHash()), false),
			Arguments.of(torrent, torrent.getMetadata().getHash(), incorrectProtocolLength, false),
			Arguments.of(torrent, torrent.getMetadata().getHash(), incorrectProtocolName, false)
		);

	}

	public static Stream<Arguments> connectionFromRemoteScenarios() {
		TorrentClient torrentClient = mock(TorrentClient.class);
		when(torrentClient.getModules()).thenReturn(Collections.emptyList());

		Torrent torrentPass = DummyEntity.createUniqueTorrent(torrentClient);
		Torrent torrent = DummyEntity.createUniqueTorrent(torrentClient);
		Torrent torrentTwo = DummyEntity.createUniqueTorrent(torrentClient, torrent);

		byte[] incorrectProtocolName = createHandshake(torrent.getMetadata().getHash());
		incorrectProtocolName[12] = 'P';

		byte[] incorrectProtocolLength = createHandshake(torrent.getMetadata().getHash());
		incorrectProtocolLength[0] = 0x12;

		return Stream.of(
			Arguments.of(torrentPass, createHandshake(torrentPass.getMetadata().getHash()), true),
			Arguments.of(torrent, createHandshake(torrentTwo.getMetadata().getHash()), false)
		);
	}

	public static byte[] createHandshake(byte[] responseHash) {
		byte[] response = new byte[] {
			// Protocol Identification
			0x13,
			// B   i     t     T     o     r     r     e    n      t    (space)
			0x42, 0x69, 0x74, 0x54, 0x6F, 0x72, 0x72, 0x65, 0x6E, 0x74, 0x20,
			// p   r     o     t     o     c     o     l
			0x70, 0x72, 0x6F, 0x74, 0x6F, 0x63, 0x6F, 0x6C,
			// Extension Bytes
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			// Torrent Hash
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
			// Peer ID
			0x01, 0x02, 0x01, 0x03, 0x05, 0x03, 0x01, 0x10, 0x20, 0x01,
			0x22, 0x12, 0x14, 0x23, 0x13, 0x04, 0x12, 0x06, 0x30, 0x01,
		};
		TestUtils.copySection(responseHash, response, 28);
		return response;
	}

	private static abstract class SelectableByteChannel extends SelectableChannel implements ByteChannel {
	}
}
