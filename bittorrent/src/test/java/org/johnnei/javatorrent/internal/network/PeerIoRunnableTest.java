package org.johnnei.javatorrent.internal.network;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.internal.torrent.TorrentManager;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;

/**
 * Tests {@link PeerIoRunnable}
 */
public class PeerIoRunnableTest extends EasyMockSupport {

	private TorrentManager torrentManager;

	private PeerIoRunnable cut;

	@Before
	public void setUp() {
		torrentManager = createMock(TorrentManager.class);
		cut = new PeerIoRunnable(torrentManager);
	}

	@Test
	public void testRun() throws Exception {
		Torrent torrent = createMock(Torrent.class);
		expect(torrentManager.getTorrents()).andReturn(Collections.singletonList(torrent));

		// Closed peer
		Peer peerOne = createMock(Peer.class);
		BitTorrentSocket socketOne = createMock(BitTorrentSocket.class);
		expect(peerOne.getBitTorrentSocket()).andReturn(socketOne).atLeastOnce();
		expect(socketOne.closed()).andReturn(true);

		// No read, write pending
		Peer peerTwo = createMock(Peer.class);
		BitTorrentSocket socketTwo = createMock(BitTorrentSocket.class);
		expect(peerTwo.getBitTorrentSocket()).andReturn(socketTwo).atLeastOnce();
		expect(socketTwo.closed()).andReturn(false);
		expect(socketTwo.hasOutboundMessages()).andReturn(true);
		socketTwo.sendMessage();
		expect(socketTwo.canReadMessage()).andReturn(false);

		// Read, queue next
		Peer peerThree = createMock(Peer.class);
		BitTorrentSocket socketThree = createMock(BitTorrentSocket.class);
		IMessage messageMock = createMock(IMessage.class);
		expect(peerThree.getBitTorrentSocket()).andReturn(socketThree).atLeastOnce();
		expect(socketThree.closed()).andReturn(false);
		expect(socketThree.hasOutboundMessages()).andReturn(false);
		expect(peerThree.getWorkQueueSize(same(PeerDirection.Upload))).andReturn(1);
		peerThree.queueNextPieceForSending();
		expect(socketThree.canReadMessage()).andReturn(true);
		expect(socketThree.readMessage()).andReturn(messageMock);
		messageMock.process(same(peerThree));

		// IOException
		Peer peerFour = createMock(Peer.class);
		BitTorrentSocket socketFour = createMock(BitTorrentSocket.class);
		expect(peerFour.getBitTorrentSocket()).andReturn(socketFour).atLeastOnce();
		expect(socketFour.closed()).andReturn(false);
		expect(socketFour.hasOutboundMessages()).andReturn(true);
		socketFour.sendMessage();
		expectLastCall().andThrow(new IOException("IOException stub"));
		socketFour.close();

		expect(torrent.getPeers()).andReturn(Arrays.asList(peerOne, peerTwo, peerThree, peerFour));

		replayAll();

		cut.run();

		verifyAll();
	}
}