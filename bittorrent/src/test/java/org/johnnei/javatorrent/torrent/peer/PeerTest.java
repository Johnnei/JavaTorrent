package org.johnnei.javatorrent.torrent.peer;

import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageCancel;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUninterested;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.Piece;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(EasyMockRunner.class)
public class PeerTest extends EasyMockSupport {

	@Test
	public void testAddModuleInfo() {
		Peer peer = DummyEntity.createPeer();

		Object o = new Object();
		peer.addModuleInfo(o);
		Object returnedO = peer.getModuleInfo(Object.class).get();

		assertEquals("Returned object is not equal to inserted", o, returnedO);
	}

	@Test(expected=IllegalStateException.class)
	public void testAddModuleInfoDuplicate() {
		Peer peer = DummyEntity.createPeer();

		Object o = new Object();
		Object o2 = new Object();
		peer.addModuleInfo(o);
		peer.addModuleInfo(o2);
	}

	@Test
	public void testAddModuleInfoNoElement() {
		Peer peer = DummyEntity.createPeer();

		Optional<Object> o = peer.getModuleInfo(Object.class);

		assertFalse("Expected empty result", o.isPresent());
	}

	@Test
	public void testDownloadInterested() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.build();
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		socketMock.enqueueMessage(isA(MessageInterested.class));

		replayAll();

		peer.setInterested(PeerDirection.Download, true);
		assertTrue("Incorrect interested state", peer.isInterested(PeerDirection.Download));

		verifyAll();
	}

	@Test
	public void testDownloadUninterested() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.build();
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		socketMock.enqueueMessage(isA(MessageUninterested.class));

		replayAll();

		peer.setInterested(PeerDirection.Download, false);
		assertFalse("Incorrect interested state", peer.isInterested(PeerDirection.Download));

		verifyAll();
	}

	@Test
	public void testUploadInterested() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.build();
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		replayAll();

		peer.setInterested(PeerDirection.Upload, true);
		assertTrue("Incorrect interested state", peer.isInterested(PeerDirection.Upload));
		peer.setInterested(PeerDirection.Upload, false);
		assertFalse("Incorrect interested state", peer.isInterested(PeerDirection.Upload));
	}

	@Test
	public void testDownloadChoke() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.build();
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		replayAll();

		peer.setChoked(PeerDirection.Download, true);
		assertTrue("Incorrect choked state", peer.isChoked(PeerDirection.Download));
		peer.setChoked(PeerDirection.Download, false);
		assertFalse("Incorrect choked state", peer.isChoked(PeerDirection.Download));

		verifyAll();
	}

	@Test
	public void testUploadChoke() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.build();
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		socketMock.enqueueMessage(isA(MessageChoke.class));

		replayAll();

		peer.setChoked(PeerDirection.Upload, true);
		assertTrue("Incorrect choked state", peer.isChoked(PeerDirection.Upload));

		verifyAll();
	}

	@Test
	public void testDownloadUnchoke() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.build();
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		socketMock.enqueueMessage(isA(MessageUnchoke.class));

		replayAll();

		peer.setChoked(PeerDirection.Upload, false);
		assertFalse("Incorrect choked state", peer.isChoked(PeerDirection.Upload));

		verifyAll();
	}

	@Test
	public void testEquality() {
		Torrent torrent = DummyEntity.createUniqueTorrent();
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);

		replayAll();

		byte[] peerId = DummyEntity.createUniquePeerId();
		byte[] peerIdTwo = DummyEntity.createUniquePeerId(peerId);

		Peer base = new Peer.Builder()
				.setSocket(socketMock)
				.setTorrent(torrent)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		Peer equal = new Peer.Builder()
				.setSocket(socketMock)
				.setTorrent(torrent)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		Peer notEqual = new Peer.Builder()
				.setSocket(socketMock)
				.setTorrent(torrent)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerIdTwo)
				.build();

		TestUtils.assertEqualityMethods(base, equal, notEqual);
	}


	@Test
	public void testAddBlockRequestDownload() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		socketMock.enqueueMessage(isA(MessageRequest.class));

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(0, 15, 15, PeerDirection.Download);

		verifyAll();

		assertEquals("Working queue should have increased", 1, peer.getWorkQueueSize(PeerDirection.Download));
	}

	@Test
	public void testAddBlockRequestUpload() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(0, 15, 15, PeerDirection.Upload);

		verifyAll();

		assertEquals("Working queue should have increased", 1, peer.getWorkQueueSize(PeerDirection.Upload));
	}

	@Test
	public void testCancelBlockRequestDownload() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		socketMock.enqueueMessage(isA(MessageRequest.class));
		socketMock.enqueueMessage(isA(MessageRequest.class));
		socketMock.enqueueMessage(isA(MessageCancel.class));

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(0, 15, 15, PeerDirection.Download);
		peer.addBlockRequest(0, 30, 15, PeerDirection.Download);
		assertEquals("Working queue should have two items", 2, peer.getWorkQueueSize(PeerDirection.Download));

		peer.cancelBlockRequest(0, 15, 15, PeerDirection.Download);

		verifyAll();

		assertEquals("Working queue should have one item anymore", 1, peer.getWorkQueueSize(PeerDirection.Download));
	}

	@Test
	public void testCancelBlockRequestUpload() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(0, 15, 15, PeerDirection.Upload);
		peer.addBlockRequest(0, 30, 15, PeerDirection.Upload);
		assertEquals("Working queue should have two items", 2, peer.getWorkQueueSize(PeerDirection.Upload));

		peer.cancelBlockRequest(0, 15, 15, PeerDirection.Upload);

		verifyAll();

		assertEquals("Working queue should have one item anymore", 1, peer.getWorkQueueSize(PeerDirection.Upload));
	}

	@Test
	public void testOnReceivedBlock() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		Piece pieceMock = createMock(Piece.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		expect(fileSetMock.getPiece(eq(0))).andReturn(pieceMock);
		expect(pieceMock.getBlockSize(eq(1))).andReturn(15);

		socketMock.enqueueMessage(isA(MessageRequest.class));
		socketMock.enqueueMessage(isA(MessageRequest.class));

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(0, 15, 15, PeerDirection.Download);
		peer.addBlockRequest(0, 30, 15, PeerDirection.Download);
		assertEquals("Working queue should have two items", 2, peer.getWorkQueueSize(PeerDirection.Download));

		peer.onReceivedBlock(0, 15);

		verifyAll();

		assertEquals("Working queue should have one item anymore", 1, peer.getWorkQueueSize(PeerDirection.Download));
	}

}
