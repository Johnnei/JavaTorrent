package org.johnnei.javatorrent.network;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.bittorrent.protocol.MessageFactory;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.internal.network.ByteInputStream;
import org.johnnei.javatorrent.internal.network.ByteOutputStream;
import org.johnnei.javatorrent.internal.network.socket.ISocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestLinkedClock;
import org.johnnei.javatorrent.test.TestUtils;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link BitTorrentSocket}
 */
public class BitTorrentSocketTest extends EasyMockSupport {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testConnect() throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 27960);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		MessageFactory messageFactoryMock = createMock(MessageFactory.class);
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);

		ISocket socketMockOne = createMock(ISocket.class);
		ISocket socketMockTwo = createMock(ISocket.class);

		expect(socketMockTwo.getInputStream()).andReturn(inputStream);
		expect(socketMockTwo.getOutputStream()).andReturn(outputStream);

		expect(connectionDegradationMock.createPreferredSocket()).andReturn(socketMockOne);
		expect(socketMockOne.isClosed()).andReturn(true);
		socketMockOne.connect(eq(socketAddress));
		expectLastCall().andThrow(new IOException("Fail on first connect"));

		expect(connectionDegradationMock.degradeSocket(same(socketMockOne))).andReturn(Optional.of(socketMockTwo));
		expect(socketMockTwo.isClosed()).andReturn(true);
		socketMockTwo.connect(eq(socketAddress));
		expect(socketMockTwo.isClosed()).andReturn(false);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);

		cut.connect(connectionDegradationMock, socketAddress);

		// Second connect call should not invoke any more calls
		cut.connect(connectionDegradationMock, socketAddress);

		verifyAll();
	}

	@Test
	public void testConnectFailure() throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 27960);

		MessageFactory messageFactoryMock = createMock(MessageFactory.class);
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);

		ISocket socketMockOne = createMock(ISocket.class);

		expect(connectionDegradationMock.createPreferredSocket()).andReturn(socketMockOne);
		expect(socketMockOne.isClosed()).andReturn(true);
		socketMockOne.connect(eq(socketAddress));
		expectLastCall().andThrow(new IOException("Fail on first connect"));

		expect(connectionDegradationMock.degradeSocket(same(socketMockOne))).andReturn(Optional.empty());

		thrown.expect(BitTorrentSocketException.class);
		thrown.expectMessage("Connection Stack");

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);

		cut.connect(connectionDegradationMock, socketAddress);

		verifyAll();
	}

	@Test
	public void testToString() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);
		BitTorrentSocket cutTwo = new BitTorrentSocket(messageFactoryMock, socketMock);

		verifyAll();

		assertTrue("Incorrect toString start", cut.toString().startsWith("BitTorrentSocket["));
		assertEquals("Incorrect socket name on null socket", "", cut.getSocketName());
		assertTrue("Incorrect socket name on nonnull socket", cutTwo.getSocketName().length() > 0);
	}

	@Test
	public void testIsClosed() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);
		expect(socketMock.isClosed()).andReturn(false);
		expect(socketMock.isClosed()).andReturn(true);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);
		BitTorrentSocket cutTwo = new BitTorrentSocket(messageFactoryMock, socketMock);

		assertTrue("Null socket is always closed.", cut.closed());
		assertFalse("Socket is should have returned that it's closed.", cutTwo.closed());
		assertTrue("Socket is should have returned that it's NOT closed.", cutTwo.closed());

		verifyAll();
	}

	@Test
	public void testGetSpeeds() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		ByteInputStream inputStream = createMock(ByteInputStream.class);
		ByteOutputStream outputStream = createMock(ByteOutputStream.class);

		expect(inputStream.pollSpeed()).andReturn(42);
		expect(outputStream.pollSpeed()).andReturn(7);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);

		Whitebox.setInternalState(cut, "inStream", inputStream);
		Whitebox.setInternalState(cut, "outStream", outputStream);

		cut.pollRates();

		verifyAll();

		assertEquals("Incorrect download speed", 42, cut.getDownloadRate());
		assertEquals("Incorrect upload speed", 7, cut.getUploadRate());

		// Assert that poll rates can be safely called on unbound socket
		BitTorrentSocket cutTwo = new BitTorrentSocket(messageFactoryMock);
		cutTwo.pollRates();
	}

	@Test
	public void testReadMessageKeepAlive() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[4]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		assertTrue("Should be able to read keep alive message", cut.canReadMessage());
		assertTrue("Incorrect message type", cut.readMessage() instanceof MessageKeepAlive);

		verifyAll();
	}

	@Test
	public void testCanReadMessageAfterThreeReads() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);
		IMessage messageMock = createMock(IMessage.class);
		messageMock.read(notNull());
		expect(messageFactoryMock.createById(eq(1))).andReturn(messageMock);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		InputStream inputStream = createMock(InputStream.class);
		expect(inputStream.available()).andReturn(2);
		expect(inputStream.available()).andReturn(4);
		expect(inputStream.available()).andReturn(0);
		expect(inputStream.available()).andReturn(1);

		Capture<byte[]> bufferCapture = EasyMock.newCapture();

		expect(inputStream.read(capture(bufferCapture), eq(0), anyInt())).andAnswer(() -> {
			bufferCapture.getValue()[3] = 1;
			return 4;
		});
		expect(inputStream.read(capture(bufferCapture), eq(0), anyInt())).andAnswer(() -> {
			bufferCapture.getValue()[0] = 1;
			return 1;
		});

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		assertFalse("Shouldn't be able to read message", cut.canReadMessage());
		assertFalse("Shouldn't be able to read message", cut.canReadMessage());
		assertTrue("Should be able to read message", cut.canReadMessage());
		assertEquals("Incorrect message type", messageMock, cut.readMessage());

		verifyAll();
	}

	@Test
	public void testCanReadMessageNotEnoughBytes() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] {
				// Length
				0x00, 0x00
		});
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		assertFalse("Should be able to read message", cut.canReadMessage());

		verifyAll();
	}

	@Test
	public void testReadMessage() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);
		IMessage messageMock = createMock(IMessage.class);
		expect(messageFactoryMock.createById(eq(1))).andReturn(messageMock);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[] {
				// Length
				0x00, 0x00, 0x00, 0x01,
				// ID
				0x01 });
		messageMock.read(notNull());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		assertTrue("Should be able to read message", cut.canReadMessage());
		assertEquals("Incorrect message type", messageMock, cut.readMessage());

		verifyAll();
	}

	@Test(expected = IllegalStateException.class)
	public void testCantReadHandshakeTwice() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		cut.readHandshake();
	}

	@Test(expected = IllegalStateException.class)
	public void testCantSendHandshakeTwice() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		cut.sendHandshake(DummyEntity.createRandomBytes(8), DummyEntity.createPeerId(), DummyEntity.createUniqueTorrentHash());
	}

	@Test(expected = IOException.class)
	public void testReadHandshakeTimeout() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		Clock baseClock = Clock.fixed(Clock.systemDefaultZone().instant(), Clock.systemDefaultZone().getZone());

		TestLinkedClock clock = new TestLinkedClock(new LinkedList<>(Arrays.asList(
				baseClock,
				baseClock,
				Clock.offset(baseClock, Duration.ofSeconds(1)),
				Clock.offset(baseClock, Duration.ofSeconds(2)),
				Clock.offset(baseClock, Duration.ofSeconds(4)),
				Clock.offset(baseClock, Duration.ofSeconds(6))
		)));

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		Whitebox.setInternalState(cut, "clock", clock);
		cut.readHandshake();
	}

	@Test(expected = IOException.class)
	public void testReadHandshakeIncorrectProtocolName() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		byte[] inputBytes = new byte[] {
				// Protocol Identification
				0x13,
				// B   i     u     T     o     r     r     e    n      t    (space)
				0x42, 0x69, 0x75, 0x54, 0x6F, 0x72, 0x72, 0x65, 0x6E, 0x74, 0x20,
				// p   r     o     u     o     c     o     l
				0x70, 0x72, 0x6F, 0x75, 0x6F, 0x63, 0x6F, 0x6C,
				// Extension Bytes
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				// Torrent Hash
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				// Peer ID
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
		};

		ByteArrayInputStream inputStream = new ByteArrayInputStream(inputBytes);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.readHandshake();

		verifyAll();
	}

	@Test(expected = IOException.class)
	public void testReadHandshakeIncorrectProtocolLength() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		byte[] inputBytes = new byte[] {
				// Protocol Identification
				0x45,
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

		ByteArrayInputStream inputStream = new ByteArrayInputStream(inputBytes);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.readHandshake();

		verifyAll();
	}

	@Test
	public void testClose() throws Exception {
		MessageFactory messageFactory = createMock(MessageFactory.class);
		ISocket socketOneMock = createMock(ISocket.class);
		ISocket socketTwoMock = createMock(ISocket.class);
		ISocket socketThreeMock = createMock(ISocket.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		expect(socketOneMock.getInputStream()).andReturn(inputStream);
		expect(socketOneMock.getOutputStream()).andReturn(outputStream);
		expect(socketTwoMock.getInputStream()).andReturn(inputStream);
		expect(socketTwoMock.getOutputStream()).andReturn(outputStream);
		expect(socketThreeMock.getInputStream()).andReturn(inputStream);
		expect(socketThreeMock.getOutputStream()).andReturn(outputStream);

		expect(socketOneMock.isClosed()).andReturn(true);
		expect(socketTwoMock.isClosed()).andReturn(false);
		expect(socketThreeMock.isClosed()).andReturn(false);
		socketTwoMock.close();
		socketThreeMock.close();
		expectLastCall().andThrow(new IOException("Test Exception path"));

		replayAll();

		new BitTorrentSocket(messageFactory, socketOneMock).close();
		new BitTorrentSocket(messageFactory, socketTwoMock).close();
		new BitTorrentSocket(messageFactory, socketThreeMock).close();

		verifyAll();
	}

	@Test
	public void testReadHandshake() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		byte[] inputBytes = new byte[] {
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

		byte[] extensionBytes = DummyEntity.createRandomBytes(8);
		byte[] peerId = DummyEntity.createPeerId();
		byte[] torrentHash = DummyEntity.createUniqueTorrentHash();

		TestUtils.copySection(extensionBytes, inputBytes, 20);
		TestUtils.copySection(torrentHash, inputBytes, 28);
		TestUtils.copySection(peerId, inputBytes, 48);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(inputBytes);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		assertFalse("Did pass handshake before handshake has completed.", cut.getPassedHandshake());
		BitTorrentHandshake handshake = cut.readHandshake();

		assertArrayEquals("Incorrect extension bytes", extensionBytes, handshake.getPeerExtensionBytes());
		assertArrayEquals("Incorrect peer id", peerId, handshake.getPeerId());
		assertArrayEquals("Incorrect torrent hash", torrentHash, handshake.getTorrentHash());

		verifyAll();
	}

	@Test
	public void testSendHandshake() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);

		byte[] expectedOutput = new byte[]{
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

		byte[] extensionBytes = DummyEntity.createRandomBytes(8);
		byte[] peerId = DummyEntity.createPeerId();
		byte[] torrentHash = DummyEntity.createUniqueTorrentHash();

		TestUtils.copySection(extensionBytes, expectedOutput, 20);
		TestUtils.copySection(torrentHash, expectedOutput, 28);
		TestUtils.copySection(peerId, expectedOutput, 48);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(expectedOutput);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = createMock(ISocket.class);
		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.sendHandshake(extensionBytes, peerId, torrentHash);

		verifyAll();

		assertArrayEquals("Incorrect output bytes", expectedOutput, outputStream.toByteArray());
	}

	@Test
	public void testEnqueueMessage() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ISocket socketMock = createMock(ISocket.class);

		expect(socketMock.getOutputStream()).andReturn(outputStream);
		expect(socketMock.getInputStream()).andReturn(inputStream);

		IMessage messageMock = createMock(IMessage.class);
		IMessage pieceMessageMock = createMock(MessageBlock.class);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);

		assertFalse("0 messages are pending, has outbound should be false", cut.hasOutboundMessages());
		cut.enqueueMessage(pieceMessageMock);
		assertTrue("Block queue must have 1 message", Whitebox.<Queue>getInternalState(cut, "blockQueue").size() == 1);
		assertTrue("1 message is pending, has outbound should be true", cut.hasOutboundMessages());

		cut.enqueueMessage(messageMock);
		assertTrue("Block queue must have 1 message", Whitebox.<Queue>getInternalState(cut, "blockQueue").size() == 1);
		assertTrue("Message queue must have 1 message", Whitebox.<Queue>getInternalState(cut, "messageQueue").size() == 1);

		verifyAll();

		assertTrue("2 messages are pending, has outbound should be true", cut.hasOutboundMessages());
	}

	@Test
	public void testSendMessageNoMessagesQueued() throws Exception {
		MessageFactory messageFactoryMock = createMock(MessageFactory.class);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ISocket socketMock = createMock(ISocket.class);

		expect(socketMock.getOutputStream()).andReturn(outputStream);
		expect(socketMock.getInputStream()).andReturn(inputStream);

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.sendMessage();

		verifyAll();
	}

	@Test
	public void testSendMessage() throws Exception {
		Clock clock = Clock.fixed(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(1)).instant(), Clock.systemDefaultZone().getZone());

		MessageFactory messageFactoryMock = createMock(MessageFactory.class);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ISocket socketMock = createMock(ISocket.class);

		expect(socketMock.getOutputStream()).andReturn(outputStream);
		expect(socketMock.getInputStream()).andReturn(inputStream);

		IMessage messageMock = createMock(MessageKeepAlive.class);
		IMessage pieceMessageMock = createMock(MessageBlock.class);

		// KeepAlive
		expect(pieceMessageMock.getId()).andReturn(BitTorrent.MESSAGE_PIECE);

		expect(messageMock.getLength()).andReturn(0).times(3);

		byte[] randomBytes = DummyEntity.createRandomBytes(5);
		expect(pieceMessageMock.getLength()).andReturn(randomBytes.length).times(3);
		Capture<OutStream> outStreamCapture = EasyMock.newCapture();
		pieceMessageMock.write(capture(outStreamCapture));
		expectLastCall().andAnswer(() -> {
			outStreamCapture.getValue().write(randomBytes);
			return null;
		});

		replayAll();

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		Whitebox.setInternalState(cut, "clock", clock);
		cut.enqueueMessage(messageMock);

		cut.enqueueMessage(pieceMessageMock);

		cut.sendMessage();

		assertArrayEquals("Incorrect keep alive output.", new byte[4], outputStream.toByteArray());
		outputStream.reset();

		cut.sendMessage();

		byte[] expectedBytes = new byte[5 + randomBytes.length];
		expectedBytes[3] = (byte) randomBytes.length;
		expectedBytes[4] = (byte) BitTorrent.MESSAGE_PIECE;
		TestUtils.copySection(randomBytes, expectedBytes, 5);
		assertArrayEquals("Incorrect piece output.", expectedBytes, outputStream.toByteArray());
		outputStream.reset();

		verifyAll();

		assertEquals("Incorrect last activity timestamp", LocalDateTime.now(clock), cut.getLastActivity());
	}
}