package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Queue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;
import org.johnnei.javatorrent.bittorrent.protocol.MessageFactory;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestClock;
import org.johnnei.javatorrent.test.TestUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.ArgumentMatchers.notNull;
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
	public void testToString() {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ISocket socketMock = mock(ISocket.class);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);

		assertTrue(cut.getSocketName().length() > 0, "Incorrect socket name on nonnull socket");
	}

	@Test
	public void testIsClosed() {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ISocket socketMock = mock(ISocket.class);
		when(socketMock.isClosed()).thenReturn(false, true);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);

		assertFalse(cut.closed(), "Socket is should have returned that it's closed.");
		assertTrue(cut.closed(), "Socket is should have returned that it's NOT closed.");
	}

	@Test
	public void testGetDownloadRate() throws IOException {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ISocket socketMock = mock(ISocket.class);
		SocketChannel channelMock = mock(SocketChannel.class);
		when(socketMock.getReadableChannel()).thenReturn(channelMock);

		mockReadMessage(messageFactoryMock, mock(IMessage.class), channelMock);

		Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		TestClock clock = new TestClock(fixedClock);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock, clock);

		clock.setClock(Clock.offset(fixedClock, Duration.ofSeconds(1)));
		cut.canReadMessage();
		cut.pollRates();

		assertThat("Incorrect speed", cut.getDownloadRate(), equalTo(5));
	}

	@Test
	public void testGetUploadRate() throws IOException {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ISocket socket = mock(ISocket.class);
		SocketChannel socketChannel = mock(SocketChannel.class);
		when(socket.getReadableChannel()).thenReturn(socketChannel);
		when(socket.getWritableChannel()).thenReturn(socketChannel);

		when(socketChannel.write((ByteBuffer) notNull())).thenAnswer(inv -> {
			ByteBuffer buffer = inv.getArgument(0);
			return buffer.remaining();
		});

		Clock fixedClock = Clock.fixed(Instant.now(), ZoneId.systemDefault());
		TestClock clock = new TestClock(fixedClock);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socket, clock);

		mockSendBlock(cut);

		clock.setClock(Clock.offset(fixedClock, Duration.ofSeconds(1)));

		cut.sendMessages();
		cut.pollRates();

		assertThat("Incorrect upload speed", cut.getUploadRate(), equalTo(10));
	}

	@Test
	public void testReadMessageKeepAlive() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		SocketChannel channel = mock(SocketChannel.class);
		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getReadableChannel()).thenReturn(channel);

		when(channel.read((ByteBuffer) isNotNull())).thenAnswer(inv -> {
			ByteBuffer buffer = inv.getArgument(0);
			buffer.put(new byte[4]);
			return 4;
		});

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		assertTrue(cut.canReadMessage(), "Should be able to read keep alive message");
		assertTrue(cut.readMessage() instanceof MessageKeepAlive, "Incorrect message type");
	}

	@Test
	public void testCanReadMessageMultipleReads() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		IMessage messageMock = mock(IMessage.class);
		when(messageFactoryMock.createById(eq(1))).thenReturn(messageMock);

		SocketChannel channel = mock(SocketChannel.class);
		ISocket socketMock = mock(ISocket.class);
		when(socketMock.getReadableChannel()).thenReturn(channel);

		when(channel.read((ByteBuffer) isNotNull())).thenAnswer(inv -> {
			ByteBuffer buffer = inv.getArgument(0);
			buffer.put(new byte[] { 0, 0, 0, 1 });
			return 4;
		}).thenAnswer(inv -> 0)
		.thenAnswer(inv -> {
			ByteBuffer buffer = inv.getArgument(0);
			buffer.put(new byte[] { 1 });
			return 1;
		});

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		assertFalse(cut.canReadMessage(), "Shouldn't be able to read message");
		assertTrue(cut.canReadMessage(), "Should be able to read message");
		assertEquals(messageMock, cut.readMessage(), "Incorrect message type");

		verify(messageMock).read(any());
	}

	@Test
	public void testCanReadMessageNotEnoughBytes() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);

		ISocket socketMock = mock(ISocket.class);
		SocketChannel channelMock = mock(SocketChannel.class);
		when(socketMock.getReadableChannel()).thenReturn(channelMock);

		when(channelMock.read((ByteBuffer) isNotNull())).thenAnswer(inv -> {
			ByteBuffer buffer = inv.getArgument(0);
			buffer.put(new byte[2]);
			return 2;
		});

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		assertThat("Length is not yet available.", cut.canReadMessage(), equalTo(false));
	}

	@Test
	public void testReadMessage() throws Exception {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		IMessage messageMock = mock(IMessage.class);

		ISocket socketMock = mock(ISocket.class);
		SocketChannel channelMock = mock(SocketChannel.class);
		when(socketMock.getReadableChannel()).thenReturn(channelMock);

		mockReadMessage(messageFactoryMock, messageMock, channelMock);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);

		assertTrue(cut.canReadMessage(), "Should be able to read message");
		assertEquals(messageMock, cut.readMessage(), "Incorrect message type");

		verify(messageMock).read(any());
	}

	@Test
	public void testClose() throws Exception {
		MessageFactory messageFactory = mock(MessageFactory.class);
		ISocket socketOneMock = mock(ISocket.class);
		ISocket socketTwoMock = mock(ISocket.class);
		ISocket socketThreeMock = mock(ISocket.class);

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
	public void testEnqueueMessage() {
		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		ISocket socketMock = mock(ISocket.class);

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
		ISocket socketMock = mock(ISocket.class);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock);
		cut.sendMessages();
	}

	@Test
	@DisplayName("testSendMessage() -> Keep alive")
	public void testSendMessage() throws Exception {
		Clock clock = Clock.fixed(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(1)).instant(), Clock.systemDefaultZone().getZone());

		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		ISocket socketMock = mock(ISocket.class);
		SocketChannel channelMock = mock(SocketChannel.class);
		when(socketMock.getReadableChannel()).thenReturn(channelMock);
		when(socketMock.getWritableChannel()).thenReturn(channelMock);

		IMessage messageMock = mock(MessageKeepAlive.class);

		// KeepAlive
		when(messageMock.getLength()).thenReturn(0);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock, clock);
		cut.enqueueMessage(messageMock);

		cut.sendMessages();

		ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(channelMock).write(bufferCaptor.capture());

		assertArrayEquals(new byte[4], ByteBufferUtils.getBytes(bufferCaptor.getValue(), 4), "Incorrect keep alive output.");
	}

	@Test
	@DisplayName("testSendMessage() -> Piece")
	public void testSendMessagePiece() throws Exception {
		Clock clock = Clock.fixed(Clock.offset(Clock.systemDefaultZone(), Duration.ofSeconds(1)).instant(), Clock.systemDefaultZone().getZone());

		MessageFactory messageFactoryMock = mock(MessageFactory.class);
		ISocket socketMock = mock(ISocket.class);
		SocketChannel channelMock = mock(SocketChannel.class);
		when(socketMock.getReadableChannel()).thenReturn(channelMock);
		when(socketMock.getWritableChannel()).thenReturn(channelMock);

		BitTorrentSocket cut = new BitTorrentSocket(messageFactoryMock, socketMock, clock);

		byte[] randomBytes = mockSendBlock(cut);

		cut.sendMessages();

		ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(channelMock).write(bufferCaptor.capture());

		byte[] expectedBytes = new byte[5 + randomBytes.length];
		expectedBytes[3] = (byte) randomBytes.length;
		expectedBytes[4] = (byte) BitTorrent.MESSAGE_PIECE;
		TestUtils.copySection(randomBytes, expectedBytes, 5);
		assertArrayEquals(expectedBytes, ByteBufferUtils.getBytes(bufferCaptor.getValue(), expectedBytes.length), "Incorrect piece output.");
		assertEquals(LocalDateTime.now(clock), cut.getLastActivity(), "Incorrect last activity timestamp");
	}

	private byte[] mockSendBlock(BitTorrentSocket cut) {
		IMessage pieceMessageMock = mock(MessageBlock.class);
		when(pieceMessageMock.getId()).thenReturn(BitTorrent.MESSAGE_PIECE);

		byte[] randomBytes = DummyEntity.createRandomBytes(5);
		when(pieceMessageMock.getLength()).thenReturn(randomBytes.length);
		ArgumentCaptor<OutStream> outStreamCapture = ArgumentCaptor.forClass(OutStream.class);
		doAnswer(inv -> {
			outStreamCapture.getValue().write(randomBytes);
			return null;
		}).when(pieceMessageMock).write(outStreamCapture.capture());

		cut.enqueueMessage(pieceMessageMock);
		return randomBytes;
	}

	private void mockReadMessage(MessageFactory messageFactoryMock, IMessage messageMock, SocketChannel channelMock) throws IOException {
		when(messageFactoryMock.createById(eq(1))).thenReturn(messageMock);
		when(channelMock.read((ByteBuffer) isNotNull())).thenAnswer(inv -> {
			ByteBuffer buffer = inv.getArgument(0);
			// Length
			buffer.put(new byte[] { 0x00, 0x00, 0x00, 0x01 });
			return 4;
		}).thenAnswer(inv -> {
			ByteBuffer buffer = inv.getArgument(0);
			// ID
			buffer.put(new byte[] { 0x01 });
			return 1;
		});
	}
}
