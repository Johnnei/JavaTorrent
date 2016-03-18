package org.johnnei.javatorrent.torrent.algos.choking;

import java.util.stream.Stream;

import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;

/**
 * Tests {@link PermissiveStrategy}
 */
@RunWith(EasyMockRunner.class)
public class PermissiveStrategyTest extends EasyMockSupport {

	@Test
	public void testUpdateChokingChoke() throws Exception {
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet filesMock = createMock(AbstractFileSet.class);
		Piece piece = new Piece(null, new byte[0], 1, 1, 1);

		expect(torrentMock.getFiles()).andStubReturn(filesMock);
		expect(filesMock.getNeededPieces()).andStubReturn(Stream.of(piece));

		Peer peerMock = createMock(Peer.class);
		expect(peerMock.getTorrent()).andReturn(torrentMock);
		expect(peerMock.hasPiece(eq(1))).andReturn(true);
		expect(peerMock.isInterested(eq(PeerDirection.Download))).andReturn(true);
		expect(peerMock.isInterested(eq(PeerDirection.Upload))).andReturn(false);
		expect(peerMock.isChoked(eq(PeerDirection.Upload))).andReturn(false);
		peerMock.setChoked(eq(PeerDirection.Upload), eq(true));

		replayAll();

		PermissiveStrategy cut = new PermissiveStrategy();
		cut.updateChoking(peerMock);

		verifyAll();
	}

	@Test
	public void testUpdateChokingUnchoke() throws Exception {
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet filesMock = createMock(AbstractFileSet.class);
		Piece piece = new Piece(null, new byte[0], 1, 1, 1);

		expect(torrentMock.getFiles()).andStubReturn(filesMock);
		expect(filesMock.getNeededPieces()).andStubReturn(Stream.of(piece));

		Peer peerMock = createMock(Peer.class);
		expect(peerMock.getTorrent()).andReturn(torrentMock);
		expect(peerMock.hasPiece(eq(1))).andReturn(true);
		expect(peerMock.isInterested(eq(PeerDirection.Download))).andReturn(true);
		expect(peerMock.isInterested(eq(PeerDirection.Upload))).andReturn(true);
		expect(peerMock.isChoked(eq(PeerDirection.Upload))).andReturn(true);
		peerMock.setChoked(eq(PeerDirection.Upload), eq(false));

		replayAll();

		PermissiveStrategy cut = new PermissiveStrategy();
		cut.updateChoking(peerMock);

		verifyAll();
	}
	@Test
	public void testUpdateChokingUpdateInterested() throws Exception {
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet filesMock = createMock(AbstractFileSet.class);
		Piece piece = new Piece(null, new byte[0], 1, 1, 1);

		expect(torrentMock.getFiles()).andStubReturn(filesMock);
		expect(filesMock.getNeededPieces()).andStubReturn(Stream.of(piece));

		Peer peerMock = createMock(Peer.class);
		expect(peerMock.getTorrent()).andReturn(torrentMock);
		expect(peerMock.hasPiece(eq(1))).andReturn(true);
		expect(peerMock.isInterested(eq(PeerDirection.Download))).andReturn(false);
		expect(peerMock.isInterested(eq(PeerDirection.Upload))).andReturn(false);
		expect(peerMock.isChoked(eq(PeerDirection.Upload))).andReturn(true);
		peerMock.setInterested(eq(PeerDirection.Download), eq(true));

		replayAll();

		PermissiveStrategy cut = new PermissiveStrategy();
		cut.updateChoking(peerMock);

		verifyAll();
	}
}