package org.johnnei.javatorrent.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.bittorrent.protocol.MessageFactory;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.network.socket.TcpSocket;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.johnnei.javatorrent.test.DummyEntity.createPeerId;
import static org.johnnei.javatorrent.test.DummyEntity.createRandomBytes;
import static org.johnnei.javatorrent.test.DummyEntity.createUniqueTorrent;
import static org.johnnei.javatorrent.test.DummyEntity.findAvailableTcpPort;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TcpPeerConnectionAcceptor}
 */
public class TcpPeerConnectionAcceptorTest {

	private TorrentClient torrentClientMock;

	private BitTorrentSocket bitTorrentSocketMock;

	private ServerSocket serverSocketMock;

	private Peer peerMock;

	private TcpPeerConnectionAcceptor cut;

	@BeforeEach
	public void setUp() {
		torrentClientMock = mock(TorrentClient.class);
	}

	private void setUpStubbedCreateSocket() throws IOException {
		bitTorrentSocketMock = mock(BitTorrentSocket.class);
		serverSocketMock = mock(ServerSocket.class);
		peerMock = mock(Peer.class);
		cut = new PartialStubbedTcpPeerAcceptor();
	}

	@Test
	public void testAcceptPeer() throws Exception {
		setUpStubbedCreateSocket();

		Socket socketMock = mock(Socket.class);
		BitTorrentHandshake handshake = new BitTorrentHandshake(createRandomBytes(20), createRandomBytes(8), createPeerId());
		Torrent torrentMock = mock(Torrent.class);
		byte[] extensionBytes = createRandomBytes(8);
		byte[] peerId = createPeerId();

		when(serverSocketMock.accept()).thenReturn(socketMock);
		when(bitTorrentSocketMock.readHandshake()).thenReturn(handshake);
		when(torrentClientMock.getTorrentByHash(aryEq(handshake.getTorrentHash()))).thenReturn(Optional.of(torrentMock));
		when(torrentClientMock.getExtensionBytes()).thenReturn(extensionBytes);
		when(torrentClientMock.getPeerId()).thenReturn(peerId);

		Metadata metadataMock = mock(Metadata.class);
		when(metadataMock.getHash()).thenReturn(handshake.getTorrentHash());
		when(torrentMock.getMetadata()).thenReturn(metadataMock);

		cut.run();

		verify(bitTorrentSocketMock).sendHandshake(aryEq(extensionBytes), aryEq(peerId), aryEq(handshake.getTorrentHash()));
		verify(torrentMock).addPeer(same(peerMock));
	}

	@Test
	public void testDenyPeer() throws Exception {
		setUpStubbedCreateSocket();

		Socket socketMock = mock(Socket.class);
		BitTorrentHandshake handshake = new BitTorrentHandshake(createRandomBytes(20), createRandomBytes(8), createPeerId());

		when(serverSocketMock.accept()).thenReturn(socketMock);
		when(bitTorrentSocketMock.readHandshake()).thenReturn(handshake);
		when(torrentClientMock.getTorrentByHash(aryEq(handshake.getTorrentHash()))).thenReturn(Optional.empty());

		cut.run();

		verify(bitTorrentSocketMock).close();
	}

	@Test
	public void testOnExceptionAccept() throws Exception {
		setUpStubbedCreateSocket();

		when(serverSocketMock.accept()).thenThrow(new IOException("Test Exception case"));

		cut.run();
	}

	@Test
	public void testOnExceptionReadHandshake() throws Exception {
		setUpStubbedCreateSocket();

		Socket socketMock = mock(Socket.class);
		when(serverSocketMock.accept()).thenReturn(socketMock);
		when(bitTorrentSocketMock.readHandshake()).thenThrow(new IOException("Test Exception case"));

		cut.run();

		verify(socketMock).close();
	}

	@Test
	public void testCreateCalls() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		torrentClientMock = mock(TorrentClient.class);
		TcpSocket socketMock = mock(TcpSocket.class);
		BitTorrentSocket bitTorrentSocketMock = mock(BitTorrentSocket.class);
		Torrent torrent = createUniqueTorrent();

		int port = findAvailableTcpPort();
		when(torrentClientMock.getDownloadPort()).thenReturn(port);
		when(torrentClientMock.getMessageFactory()).thenReturn(messageFactoryMock);
		when(socketMock.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
		when(socketMock.getOutputStream()).thenReturn(new ByteArrayOutputStream());

		cut = new TcpPeerConnectionAcceptor(torrentClientMock);
		assertNotNull(cut.createSocket(socketMock), "Didn't create a socket");
		cut.createPeer(bitTorrentSocketMock, torrent, createRandomBytes(8), createPeerId());
	}

	private class PartialStubbedTcpPeerAcceptor extends TcpPeerConnectionAcceptor {

		public PartialStubbedTcpPeerAcceptor() throws IOException {
			super(torrentClientMock);
		}

		@Override
		ServerSocket createServerSocket() {
			return serverSocketMock;
		}

		@Override
		BitTorrentSocket createSocket(ISocket socket) throws IOException {
			assertNotNull(socket);
			return bitTorrentSocketMock;
		}

		@Override
		Peer createPeer(BitTorrentSocket socket, Torrent torrent, byte[] extensionBytes, byte[] peerId) {
			assertAll(
				() -> assertEquals(bitTorrentSocketMock, socket, "Incorrect socket"),
				() -> assertNotNull(torrent, "Creating peer with null torrent"),
				() -> assertNotNull(extensionBytes, "Creating peer with null extension bytes"),
				() -> assertNotNull(peerId, "Creating peer with null id bytes")
			);
			return peerMock;
		}
	}
}
