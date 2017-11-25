package org.johnnei.javatorrent.internal.network;

import java.io.IOException;
import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.internal.torrent.TorrentManager;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PeerIoRunnable}
 */
@RunWith(MockitoJUnitRunner.class)
public class PeerIoRunnableTest {

	@Mock
	private TorrentManager torrentManager;

	@InjectMocks
	private PeerIoRunnable cut;

	@Test
	public void testHandleClosedSocket() throws Exception {
		Torrent torrent = mock(Torrent.class);
		when(torrentManager.getTorrents()).thenReturn(Collections.singletonList(torrent));

		Peer peer = mock(Peer.class);
		BitTorrentSocket socket = mock(BitTorrentSocket.class);
		when(peer.getBitTorrentSocket()).thenReturn(socket);
		when(socket.closed()).thenReturn(true);

		when(torrent.getPeers()).thenReturn(Collections.singletonList(peer));

		cut.run();

		verify(peer).getBitTorrentSocket();
		verifyNoMoreInteractions(peer);
	}

	@Test
	public void testHandleWritePending() throws Exception {
		Torrent torrent = mock(Torrent.class);
		when(torrentManager.getTorrents()).thenReturn(Collections.singletonList(torrent));

		Peer peer = mock(Peer.class);
		BitTorrentSocket socket = mock(BitTorrentSocket.class);
		when(peer.getBitTorrentSocket()).thenReturn(socket);
		when(socket.closed()).thenReturn(false);
		when(socket.hasOutboundMessages()).thenReturn(true);
		when(socket.canReadMessage()).thenReturn(false);

		when(torrent.getPeers()).thenReturn(Collections.singletonList(peer));

		cut.run();

		verify(socket).sendMessage();
	}

	@Test
	public void testQueueNextBlock() throws Exception {
		Torrent torrent = mock(Torrent.class);
		when(torrentManager.getTorrents()).thenReturn(Collections.singletonList(torrent));

		Peer peer = mock(Peer.class);
		BitTorrentSocket socket = mock(BitTorrentSocket.class);
		IMessage messageMock = mock(IMessage.class);
		when(peer.getBitTorrentSocket()).thenReturn(socket);
		when(socket.closed()).thenReturn(false);
		when(socket.hasOutboundMessages()).thenReturn(false);
		when(peer.getWorkQueueSize(same(PeerDirection.Upload))).thenReturn(1);
		when(socket.canReadMessage()).thenReturn(true);
		when(socket.readMessage()).thenReturn(messageMock);

		when(torrent.getPeers()).thenReturn(Collections.singletonList(peer));

		cut.run();

		verify(peer).queueNextPieceForSending();
		verify(messageMock).process(same(peer));
	}

	@Test
	public void testPeerCauseException() throws Exception {
		Torrent torrent = mock(Torrent.class);
		when(torrentManager.getTorrents()).thenReturn(Collections.singletonList(torrent));

		Peer peer = mock(Peer.class);
		BitTorrentSocket socket = mock(BitTorrentSocket.class);
		when(peer.getBitTorrentSocket()).thenReturn(socket);
		when(socket.closed()).thenReturn(false);
		when(socket.hasOutboundMessages()).thenReturn(true);
		doThrow(new IOException("IOException stub")).when(socket).sendMessage();
		when(peer.getTorrent()).thenReturn(torrent);

		when(torrent.getPeers()).thenReturn(Collections.singletonList(peer));

		cut.run();

		verify(torrent).removePeer(same(peer));
		verify(socket).close();
	}
}
