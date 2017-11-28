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

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentHandshake;
import org.johnnei.javatorrent.bittorrent.protocol.MessageFactory;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.internal.network.ByteInputStream;
import org.johnnei.javatorrent.internal.network.ByteOutputStream;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestLinkedClock;
import org.johnnei.javatorrent.test.TestUtils;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link BitTorrentSocket}
 */
public class BitTorrentSocketTest {

	@Test
	public void testConnect() throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 27960);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		ConnectionDegradation connectionDegradationMock = mock(ConnectionDegradation.class);

		ISocket socketMockOne = mock(ISocket.class);
		ISocket socketMockTwo = mock(ISocket.class);

		when(socketMockTwo.getInputStream()).thenReturn(inputStream);
		when(socketMockTwo.getOutputStream()).thenReturn(outputStream);

		when(connectionDegradationMock.createPreferredSocket()).thenReturn(socketMockOne);
		when(socketMockOne.isClosed()).thenReturn(true);

		doThrow(new IOException("Fail on first connect")).when(socketMockOne).connect(eq(socketAddress));

		when(connectionDegradationMock.degradeSocket(same(socketMockOne))).thenReturn(Optional.of(socketMockTwo));
		when(socketMockTwo.isClosed()).thenReturn(true).thenReturn(false);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);

		cut.connect(connectionDegradationMock, socketAddress);

		// Second connect call should not invoke any more calls
		cut.connect(connectionDegradationMock, socketAddress);

		verify(socketMockTwo).connect(eq(socketAddress));
	}

	@Test
	public void testConnectFailure() throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 27960);

		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		ConnectionDegradation connectionDegradationMock = mock(ConnectionDegradation.class);

		ISocket socketMockOne = mock(ISocket.class);

		when(connectionDegradationMock.createPreferredSocket()).thenReturn(socketMockOne);
		when(socketMockOne.isClosed()).thenReturn(true);
		doThrow(new IOException("Fail on first connect")).when(socketMockOne).connect(eq(socketAddress));

		when(connectionDegradationMock.degradeSocket(same(socketMockOne))).thenReturn(Optional.empty());

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);

		Exception e = assertThrows(BitTorrentSocketException.class, () -> cut.connect(connectionDegradationMock, socketAddress));
		assertThat(e.getMessage(), containsString("Connection Stack"));
	}

	@Test
	public void testToString() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);
		BitTorrentSocket cutTwo = new BitTorrentSocket(messageFactoryMock, socketMock);

		assertTrue(cut.toString().startsWith("BitTorrentSocket["), "Incorrect toString start");
		assertEquals("", cut.getSocketName(), "Incorrect socket name on null socket");
		assertTrue(cutTwo.getSocketName().length() > 0, "Incorrect socket name on nonnull socket");
	}

	@Test
	public void testIsClosed() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);
		when(socketMock.isClosed()).thenReturn(false, true);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);
		BitTorrentSocket cutTwo = new BitTorrentSocket(messageFactoryMock, socketMock);

		assertTrue(cut.closed(), "Null socket is always closed.");
		assertFalse(cutTwo.closed(), "Socket is should have returned that it's closed.");
		assertTrue(cutTwo.closed(), "Socket is should have returned that it's NOT closed.");
	}

	@Test
	public void testGetSpeeds() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ByteInputStream inputStream = mock(ByteInputStream.class);
		ByteOutputStream outputStream = mock(ByteOutputStream.class);

		when(inputStream.pollSpeed()).thenReturn(42);
		when(outputStream.pollSpeed()).thenReturn(7);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock);

		Whitebox.setInternalState(cut, "inStream", inputStream);
		Whitebox.setInternalState(cut, "outStream", outputStream);

		cut.pollRates();

		assertEquals(42, cut.getDownloadRate(), "Incorrect download speed");
		assertEquals(7, cut.getUploadRate(), "Incorrect upload speed");

		// Assert that poll rates can be safely called on unbound socket
		BitTorrentSocket cutTwo = new BitTorrentSocket(messageFactoryMock);
		cutTwo.pollRates();
	}

	@Test
	public void testReadMessageKeepAlive() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[4]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		assertTrue(cut.canReadMessage(), "Should be able to read keep alive message");
		assertTrue(cut.readMessage() instanceof MessageKeepAlive, "Incorrect message type");
	}

	@Test
	public void testCanReadMessageAfterThreeReads() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		IMessage messageMock = mock(IMessage.class);
		when(messageFactoryMock.createById(eq(1))).thenReturn(messageMock);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		InputStream inputStream = mock(InputStream.class);
		when(inputStream.available()).thenReturn(2, 4, 0, 1);

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		ArgumentCaptor<byte[]> bufferCapture = ArgumentCaptor.forClass(byte[].class);
		when(inputStream.read(bufferCapture.capture(), eq(0), anyInt())).thenAnswer(inv -> {
			bufferCapture.getValue()[3] = 1;
			return 4;
		}).thenAnswer(inv -> {
			bufferCapture.getValue()[0] = 1;
			return 1;
		});

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		assertFalse(cut.canReadMessage(), "Shouldn't be able to read message");
		assertFalse(cut.canReadMessage(), "Shouldn't be able to read message");
		assertTrue(cut.canReadMessage(), "Should be able to read message");
		assertEquals(messageMock, cut.readMessage(), "Incorrect message type");

		verify(messageMock).read(any());
	}

	@Test
	public void testCanReadMessageNotEnoughBytes() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{
			// Length
			0x00, 0x00
		});
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		assertFalse(cut.canReadMessage(), "Should be able to read message");
	}

	@Test
	public void testReadMessage() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		IMessage messageMock = mock(IMessage.class);
		when(messageFactoryMock.createById(eq(1))).thenReturn(messageMock);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[]{
			// Length
			0x00, 0x00, 0x00, 0x01,
			// ID
			0x01 });
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();

		assertTrue(cut.canReadMessage(), "Should be able to read message");
		assertEquals(messageMock, cut.readMessage(), "Incorrect message type");

		verify(messageMock).read(any());
	}

	@Test
	public void testCantReadHandshakeTwice() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		assertThrows(IllegalStateException.class, cut::readHandshake);
	}

	@Test
	public void testCantSendHandshakeTwice() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.setPassedHandshake();
		assertThrows(
			IllegalStateException.class,
			() -> cut.sendHandshake(DummyEntity.createRandomBytes(8), DummyEntity.createPeerId(), DummyEntity.createUniqueTorrentHash())
		);
	}

	@Test
	public void testReadHandshakeTimeout() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		Clock baseClock = Clock.fixed(Clock.systemDefaultZone().instant(), Clock.systemDefaultZone().getZone());

		TestLinkedClock clock = new TestLinkedClock(new LinkedList<>(Arrays.asList(
			baseClock,
			baseClock,
			Clock.offset(baseClock, Duration.ofSeconds(1)),
			Clock.offset(baseClock, Duration.ofSeconds(2)),
			Clock.offset(baseClock, Duration.ofSeconds(4)),
			Clock.offset(baseClock, Duration.ofSeconds(6))
		)));

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		Whitebox.setInternalState(cut, "clock", clock);
		assertThrows(IOException.class, cut::readHandshake);
	}

	@Test
	public void testReadHandshakeIncorrectProtocolName() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		byte[] inputBytes = new byte[]{
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

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		assertThrows(IOException.class, cut::readHandshake);
	}

	@Test
	public void testReadHandshakeIncorrectProtocolLength() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		byte[] inputBytes = new byte[]{
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

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		assertThrows(IOException.class, cut::readHandshake);
	}

	@Test
	public void testClose() throws Exception {
		MessageFactory messageFactory = mock(MessageFactory.class);
		ISocket socketOneMock = mock(ISocket.class);
		ISocket socketTwoMock = mock(ISocket.class);
		ISocket socketThreeMock = mock(ISocket.class);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		when(socketOneMock.getInputStream()).thenReturn(inputStream);
		when(socketOneMock.getOutputStream()).thenReturn(outputStream);
		when(socketTwoMock.getInputStream()).thenReturn(inputStream);
		when(socketTwoMock.getOutputStream()).thenReturn(outputStream);
		when(socketThreeMock.getInputStream()).thenReturn(inputStream);
		when(socketThreeMock.getOutputStream()).thenReturn(outputStream);

		when(socketOneMock.isClosed()).thenReturn(true);
		when(socketTwoMock.isClosed()).thenReturn(false);
		when(socketThreeMock.isClosed()).thenReturn(false);
		doThrow(new IOException("Test Exception path")).when(socketThreeMock).close();

		new BitTorrentSocket(messageFactory, socketOneMock).close();
		new BitTorrentSocket(messageFactory, socketTwoMock).close();
		new BitTorrentSocket(messageFactory, socketThreeMock).close();

		verify(socketTwoMock).close();
	}

	@Test
	public void testReadHandshake() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		byte[] inputBytes = new byte[]{
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

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		assertFalse(cut.getPassedHandshake(), "Did pass handshake before handshake has completed.");
		BitTorrentHandshake handshake = cut.readHandshake();

		assertAll(
			() -> assertArrayEquals(extensionBytes, handshake.getPeerExtensionBytes(), "Incorrect extension bytes"),
			() -> assertArrayEquals(peerId, handshake.getPeerId(), "Incorrect peer id"),
			() -> assertArrayEquals(torrentHash, handshake.getTorrentHash(), "Incorrect torrent hash")
		);
	}

	@Test
	public void testSendHandshake() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		byte[] whenedOutput = new byte[]{
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

		TestUtils.copySection(extensionBytes, whenedOutput, 20);
		TestUtils.copySection(torrentHash, whenedOutput, 28);
		TestUtils.copySection(peerId, whenedOutput, 48);

		ByteArrayInputStream inputStream = new ByteArrayInputStream(whenedOutput);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.sendHandshake(extensionBytes, peerId, torrentHash);

		assertArrayEquals(whenedOutput, outputStream.toByteArray(), "Incorrect output bytes");
	}

	@Test
	public void testEnqueueMessage() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ISocket socketMock = mock(ISocket.class);

		when(socketMock.getOutputStream()).thenReturn(outputStream);
		when(socketMock.getInputStream()).thenReturn(inputStream);

		IMessage messageMock = mock(IMessage.class);
		IMessage pieceMessageMock = mock(MessageBlock.class);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);

		assertFalse(cut.hasOutboundMessages(), "0 messages are pending, has outbound should be false");
		cut.enqueueMessage(pieceMessageMock);
		assertTrue(Whitebox.<Queue>getInternalState(cut, "blockQueue").size() == 1, "Block queue must have 1 message");
		assertTrue(cut.hasOutboundMessages(), "1 message is pending, has outbound should be true");

		cut.enqueueMessage(messageMock);
		assertTrue(Whitebox.<Queue>getInternalState(cut, "blockQueue").size() == 1, "Block queue must have 1 message");
		assertTrue(Whitebox.<Queue>getInternalState(cut, "messageQueue").size() == 1, "Message queue must have 1 message");

		assertTrue(cut.hasOutboundMessages(), "2 messages are pending, has outbound should be true");
	}

	@Test
	public void testSendMessageNoMessagesQueued() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ISocket socketMock = mock(ISocket.class);

		when(socketMock.getOutputStream()).thenReturn(outputStream);
		when(socketMock.getInputStream()).thenReturn(inputStream);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.sendMessage();
	}

	@Test
	public void testSendMessage() throws Exception {
		Clock clock = Clock.fixed(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(1)).instant(), Clock.systemDefaultZone().getZone());

		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		ByteArrayInputStream inputStream = new ByteArrayInputStream(new byte[0]);
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ISocket socketMock = mock(ISocket.class);

		when(socketMock.getOutputStream()).thenReturn(outputStream);
		when(socketMock.getInputStream()).thenReturn(inputStream);

		IMessage messageMock = mock(MessageKeepAlive.class);
		IMessage pieceMessageMock = mock(MessageBlock.class);

		// KeepAlive
		when(pieceMessageMock.getId()).thenReturn(BitTorrent.MESSAGE_PIECE);

		when(messageMock.getLength()).thenReturn(0);

		byte[] randomBytes = DummyEntity.createRandomBytes(5);
		when(pieceMessageMock.getLength()).thenReturn(randomBytes.length);
		ArgumentCaptor<OutStream> outStreamCapture = ArgumentCaptor.forClass(OutStream.class);
		doAnswer(inv -> {
			outStreamCapture.getValue().write(randomBytes);
			return null;
		}).when(pieceMessageMock).write(outStreamCapture.capture());

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		Whitebox.setInternalState(cut, "clock", clock);
		cut.enqueueMessage(messageMock);

		cut.enqueueMessage(pieceMessageMock);

		cut.sendMessage();

		assertArrayEquals(new byte[4], outputStream.toByteArray(), "Incorrect keep alive output.");
		outputStream.reset();

		cut.sendMessage();

		byte[] whenedBytes = new byte[5 + randomBytes.length];
		whenedBytes[3] = (byte) randomBytes.length;
		whenedBytes[4] = (byte) BitTorrent.MESSAGE_PIECE;
		TestUtils.copySection(randomBytes, whenedBytes, 5);
		assertArrayEquals(whenedBytes, outputStream.toByteArray(), "Incorrect piece output.");
		assertEquals(LocalDateTime.now(clock), cut.getLastActivity(), "Incorrect last activity timestamp");
	}
}
