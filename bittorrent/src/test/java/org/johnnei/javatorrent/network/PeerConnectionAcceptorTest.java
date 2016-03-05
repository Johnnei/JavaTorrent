package org.johnnei.javatorrent.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.bittorrent.protocol.MessageFactory;
import org.johnnei.javatorrent.network.socket.TcpSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.johnnei.javatorrent.test.DummyEntity.createPeerId;
import static org.johnnei.javatorrent.test.DummyEntity.createRandomBytes;
import static org.johnnei.javatorrent.test.DummyEntity.createTorrent;
import static org.johnnei.javatorrent.test.DummyEntity.findAvailableTcpPort;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Tests {@link PeerConnectionAcceptor}
 */
public class PeerConnectionAcceptorTest extends EasyMockSupport {

	private TorrentClient torrentClientMock;

	private BitTorrentSocket bitTorrentSocketMock;

	private ServerSocket serverSocketMock;

	private Peer peerMock;

	private PeerConnectionAcceptor cut;

	@Before
	public void setUp() {
		torrentClientMock = createMock(TorrentClient.class);
	}

	private void setUpStubbedCreateSocket() throws IOException {
		bitTorrentSocketMock = createMock(BitTorrentSocket.class);
		serverSocketMock = createMock(ServerSocket.class);
		peerMock = createMock(Peer.class);
		cut = new PartialStubbedPeerAcceptor();
	}

	@Test
	public void testAcceptPeer() throws Exception {
		setUpStubbedCreateSocket();

		Socket socketMock = createMock(Socket.class);
		BitTorrentHandshake handshake = new BitTorrentHandshake(createRandomBytes(20), createRandomBytes(8), createPeerId());
		Torrent torrentMock = createMock(Torrent.class);
		byte[] extensionBytes = createRandomBytes(8);
		byte[] peerId = createPeerId();

		expect(serverSocketMock.accept()).andReturn(socketMock);
		expect(bitTorrentSocketMock.readHandshake()).andReturn(handshake);
		expect(torrentClientMock.getTorrentByHash(aryEq(handshake.getTorrentHash()))).andReturn(Optional.of(torrentMock));
		expect(torrentClientMock.getExtensionBytes()).andReturn(extensionBytes);
		expect(torrentClientMock.getPeerId()).andReturn(peerId);
		expect(torrentMock.getHashArray()).andReturn(handshake.getTorrentHash());
		bitTorrentSocketMock.sendHandshake(aryEq(extensionBytes), aryEq(peerId), aryEq(handshake.getTorrentHash()));
		torrentMock.addPeer(same(peerMock));

		replayAll();

		cut.run();

		verifyAll();
	}

	@Test
	public void testDenyPeer() throws Exception {
		setUpStubbedCreateSocket();

		Socket socketMock = createMock(Socket.class);
		BitTorrentHandshake handshake = new BitTorrentHandshake(createRandomBytes(20), createRandomBytes(8), createPeerId());

		expect(serverSocketMock.accept()).andReturn(socketMock);
		expect(bitTorrentSocketMock.readHandshake()).andReturn(handshake);
		expect(torrentClientMock.getTorrentByHash(aryEq(handshake.getTorrentHash()))).andReturn(Optional.empty());
		bitTorrentSocketMock.close();

		replayAll();

		cut.run();

		verifyAll();
	}

	@Test
	public void testOnExceptionAccept() throws Exception {
		setUpStubbedCreateSocket();

		expect(serverSocketMock.accept()).andThrow(new IOException("Test Exception case"));

		replayAll();

		cut.run();

		verifyAll();
	}

	@Test
	public void testOnExceptionReadHandshake() throws Exception {
		setUpStubbedCreateSocket();

		Socket socketMock = createMock(Socket.class);

		expect(serverSocketMock.accept()).andReturn(socketMock);
		expect(bitTorrentSocketMock.readHandshake()).andThrow(new IOException("Test Exception case"));
		socketMock.close();

		replayAll();

		cut.run();

		verifyAll();
	}

	@Test
	public void testCreateCalls() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);
		torrentClientMock = createMock(TorrentClient.class);
		TcpSocket socketMock = createMock(TcpSocket.class);
		BitTorrentSocket bitTorrentSocketMock = createMock(BitTorrentSocket.class);
		Torrent torrent = createTorrent();

		int port = findAvailableTcpPort();
		expect(torrentClientMock.getDownloadPort()).andReturn(port);
		expect(torrentClientMock.getMessageFactory()).andReturn(messageFactoryMock);
		expect(socketMock.getInputStream()).andReturn(new ByteArrayInputStream(new byte[0]));
		expect(socketMock.getOutputStream()).andReturn(new ByteArrayOutputStream());

		replayAll();

		try {
			cut = new PeerConnectionAcceptor(torrentClientMock);
			assertNotNull("Didn't create a socket", cut.createSocket(socketMock));
			cut.createPeer(bitTorrentSocketMock, torrent, createRandomBytes(8));
		} finally {
			verifyAll();
		}

	}

	private class PartialStubbedPeerAcceptor extends PeerConnectionAcceptor {

		public PartialStubbedPeerAcceptor() throws IOException {
			super(torrentClientMock);
		}

		@Override
		ServerSocket createServerSocket() {
			return serverSocketMock;
		}

		@Override
		BitTorrentSocket createSocket(TcpSocket socket) throws IOException {
			assertNotNull(socket);
			return bitTorrentSocketMock;
		}

		@Override
		Peer createPeer(BitTorrentSocket socket, Torrent torrent, byte[] extensionBytes) {
			assertEquals("Incorrect socket", bitTorrentSocketMock, socket);
			assertNotNull("Creating peer with null torrent", torrent);
			assertNotNull("Creating peer with null extension bytes");
			return peerMock;
		}
	}
}