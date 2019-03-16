package org.johnnei.javatorrent.internal.network;

import java.io.IOException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrentProtocolViolationException;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.socket.ISocket;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PeerIoHandlerTest {

	private PeerIoHandler cut;

	private ScheduledExecutorService scheduler;

	private Runnable pollTask;

	private ScheduledFuture scheduledFuture;

	@BeforeEach
	public void setUp() {
		scheduler = mock(ScheduledExecutorService.class);
		scheduledFuture = mock(ScheduledFuture.class);

		ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);

		when(scheduler.scheduleWithFixedDelay(runnableCaptor.capture(), eq(50L), eq(50L), eq(TimeUnit.MILLISECONDS)))
			.thenReturn(scheduledFuture);

		cut = new PeerIoHandler(scheduler);
		pollTask = runnableCaptor.getValue();
	}

	@AfterEach
	public void tearDown() {
		cut.shutdown();
		verify(scheduledFuture).cancel(false);
	}

	@Test
	@DisplayName("testRegisterPeer() - Single underlying Channel")
	public void testRegisterPeer() throws Exception {
		Peer peer = mock(Peer.class);
		ISocket<SocketChannel, SocketChannel> socket = mock(ISocket.class);
		try (SocketChannel channel = SocketChannel.open()) {
			channel.configureBlocking(false);
			when(socket.getReadableChannel()).thenReturn(channel);
			when(socket.getWritableChannel()).thenReturn(channel);
			cut.registerPeer(peer, socket);
		}

		verify(socket, atLeast(1)).getReadableChannel();
		verify(socket, never()).getWritableChannel();
	}

	@Test
	@DisplayName("testRegisterPeer() - Dual underlying Channels")
	public void testRegisterPeerDual() throws Exception {
		Peer peer = mock(Peer.class);
		ISocket<Pipe.SourceChannel, Pipe.SinkChannel> socket = mock(ISocket.class);
		Pipe pipe = Pipe.open();
		pipe.source().configureBlocking(false);
		pipe.sink().configureBlocking(false);
		when(socket.getReadableChannel()).thenReturn(pipe.source());
		when(socket.getWritableChannel()).thenReturn(pipe.sink());
		cut.registerPeer(peer, socket);

		verify(socket, atLeast(1)).getReadableChannel();
		verify(socket, atLeast(1)).getWritableChannel();
	}

	@Test
	@DisplayName("testHandlePeer() - Read")
	public void testHandlePeerRead() throws Exception {
		Peer peer = mock(Peer.class);
		SelectionKey key = mock(SelectionKey.class);
		BitTorrentSocket socket = mock(BitTorrentSocket.class);
		IMessage message = mock(IMessage.class);

		when(peer.getBitTorrentSocket()).thenReturn(socket);
		when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
		when(socket.canReadMessage()).thenReturn(true, false);
		when(socket.readMessage()).thenReturn(message);

		cut.handlePeer(key, peer);

		verify(message).process(peer);
	}

	@Test
	@DisplayName("testHandlePeer() - Read - Protocol Error")
	public void testHandlePeerReadViolation() throws Exception {
		Peer peer = mock(Peer.class);
		SelectionKey key = mock(SelectionKey.class);
		BitTorrentSocket socket = mock(BitTorrentSocket.class);

		when(peer.getBitTorrentSocket()).thenReturn(socket);
		when(key.readyOps()).thenReturn(SelectionKey.OP_READ);
		when(socket.canReadMessage()).thenReturn(true, false);
		when(socket.readMessage()).thenThrow(new BitTorrentProtocolViolationException("Kapot"));

		cut.handlePeer(key, peer);

		verify(socket).close();
	}

	@Test
	@DisplayName("testHandlePeer() - Write")
	public void testHandlePeerWrite() throws Exception {
		Peer peer = mock(Peer.class);
		SelectionKey key = mock(SelectionKey.class);
		BitTorrentSocket socket = mock(BitTorrentSocket.class);

		when(peer.getBitTorrentSocket()).thenReturn(socket);
		when(key.readyOps()).thenReturn(SelectionKey.OP_WRITE);
		when(socket.hasOutboundMessages()).thenReturn(true, false);

		cut.handlePeer(key, peer);

		verify(socket).sendMessages();
	}

	@Test
	@DisplayName("testHandlePeer() - Write - Exception")
	public void testHandlePeerWriteException() throws Exception {
		Peer peer = mock(Peer.class);
		SelectionKey key = mock(SelectionKey.class);
		BitTorrentSocket socket = mock(BitTorrentSocket.class);

		when(peer.getBitTorrentSocket()).thenReturn(socket);
		when(key.readyOps()).thenReturn(SelectionKey.OP_WRITE);
		when(socket.hasOutboundMessages()).thenReturn(true, false);

		doThrow(new IOException("Kapot")).when(socket).sendMessages();

		cut.handlePeer(key, peer);

		verify(socket).close();
	}

}
