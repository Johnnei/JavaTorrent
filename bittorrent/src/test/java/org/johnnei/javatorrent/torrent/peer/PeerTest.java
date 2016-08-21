package org.johnnei.javatorrent.torrent.peer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageCancel;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUninterested;
import org.johnnei.javatorrent.disk.DiskJobReadBlock;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(EasyMockRunner.class)
public class PeerTest extends EasyMockSupport {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testBuilderSetExtensionBytesIncorrectLength() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Extension bytes");

		new Peer.Builder().setExtensionBytes(new byte[9]);
	}

	@Test
	public void testBuilderSetIdIncorrectLength() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("Id bytes");

		new Peer.Builder().setId(new byte[9]);
	}

	@Test
	public void testAddModuleInfo() {
		Peer peer = DummyEntity.createPeer();

		Object o = new Object();
		peer.addModuleInfo(o);
		Object returnedO = peer.getModuleInfo(Object.class).get();

		assertEquals("Returned object is not equal to inserted", o, returnedO);
	}

	@Test(expected = IllegalStateException.class)
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
		Piece pieceMock = createMock(Piece.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		socketMock.enqueueMessage(isA(MessageRequest.class));
		expect(pieceMock.getIndex()).andReturn(0);

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Download);

		verifyAll();

		assertEquals("Working queue should have increased", 1, peer.getWorkQueueSize(PeerDirection.Download));
	}

	@Test
	public void testAddBlockRequestUpload() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		Piece pieceMock = createMock(Piece.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Upload);

		verifyAll();

		assertEquals("Working queue should have increased", 1, peer.getWorkQueueSize(PeerDirection.Upload));
	}

	@Test
	public void testCancelBlockRequestDownload() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		Piece pieceMock = createMock(Piece.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		socketMock.enqueueMessage(isA(MessageRequest.class));
		socketMock.enqueueMessage(isA(MessageRequest.class));
		socketMock.enqueueMessage(isA(MessageCancel.class));
		expect(fileSetMock.getPiece(0)).andReturn(pieceMock);
		expect(pieceMock.getIndex()).andReturn(0).atLeastOnce();

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Download);
		peer.addBlockRequest(pieceMock, 30, 15, PeerDirection.Download);
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
		Piece pieceMock = createMock(Piece.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		expect(fileSetMock.getPiece(0)).andReturn(pieceMock);

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Upload);
		peer.addBlockRequest(pieceMock, 30, 15, PeerDirection.Upload);
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
		expect(pieceMock.getIndex()).andReturn(0).atLeastOnce();

		socketMock.enqueueMessage(isA(MessageRequest.class));
		socketMock.enqueueMessage(isA(MessageRequest.class));

		replayAll();

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Download);
		peer.addBlockRequest(pieceMock, 30, 15, PeerDirection.Download);
		assertEquals("Working queue should have two items", 2, peer.getWorkQueueSize(PeerDirection.Download));

		peer.onReceivedBlock(0, 15);

		verifyAll();

		assertEquals("Working queue should have one item anymore", 1, peer.getWorkQueueSize(PeerDirection.Download));
	}

	@Test
	public void testQueueNextPieceForSendingNoPendingJobs() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.queueNextPieceForSending();

		verifyAll();
	}

	@Test
	public void testQueueNextPieceForSendingSingleRequestOnly() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		Piece pieceMock = createMock(Piece.class);
		Capture<DiskJobReadBlock> diskJobCapture = EasyMock.newCapture();

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		torrentMock.addDiskJob(capture(diskJobCapture));

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.addBlockRequest(pieceMock, 0, 15, PeerDirection.Upload);
		cut.queueNextPieceForSending();
		cut.queueNextPieceForSending();

		verifyAll();
	}

	@Test
	public void testQueueNextPieceForSending() throws Exception {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		Piece pieceMock = createMock(Piece.class);
		Capture<DiskJobReadBlock> diskJobCapture = EasyMock.newCapture();

		socketMock.enqueueMessage(isA(MessageBlock.class));
		torrentMock.addUploadedBytes(15);
		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		torrentMock.addDiskJob(capture(diskJobCapture));
		expect(pieceMock.getIndex()).andStubReturn(0);
		expect(pieceMock.loadPiece(eq(0), eq(15))).andReturn(new byte[15]);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.addBlockRequest(pieceMock, 0, 15, PeerDirection.Upload);
		cut.queueNextPieceForSending();

		diskJobCapture.getValue().process();

		verifyAll();
	}

	@Test
	public void testDiscardAllBlockRequests() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		Piece pieceMock = createMock(Piece.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		expect(pieceMock.getIndex()).andReturn(0).atLeastOnce();
		pieceMock.setBlockStatus(eq(0), eq(BlockStatus.Needed));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Needed));

		socketMock.enqueueMessage(isA(MessageRequest.class));
		socketMock.enqueueMessage(isA(MessageRequest.class));

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.addBlockRequest(pieceMock, 0, 15, PeerDirection.Download);
		cut.addBlockRequest(pieceMock, 15, 15, PeerDirection.Download);

		assertEquals("Working queue should have two items", 2, cut.getWorkQueueSize(PeerDirection.Download));

		cut.discardAllBlockRequests();

		verifyAll();
		assertEquals("Working queue should have two items", 0, cut.getWorkQueueSize(PeerDirection.Download));
	}

	@Test
	public void testSetHasPiece() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(torrentMock.isDownloadingMetadata()).andStubReturn(false);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		assertFalse("Piece 0 should not be available yet", cut.hasPiece(0));
		assertEquals("No pieces should be completed yet", 0, cut.countHavePieces());

		cut.setHavingPiece(0);

		assertTrue("Piece 0 should be available", cut.hasPiece(0));
		assertEquals("One pieces should be completed", 1, cut.countHavePieces());
	}

	@Test
	public void testSetGetRequestLimit() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		assertEquals("Initial request limit should have been 1", 1, cut.getRequestLimit());

		cut.setRequestLimit(7);
		assertEquals("Request limit should have changed", 7, cut.getRequestLimit());
		cut.setRequestLimit(-1);
		assertEquals("Request limit should not have changed", 7, cut.getRequestLimit());

		cut.setAbsoluteRequestLimit(5);
		assertEquals("Request limit should have been limited", 5, cut.getRequestLimit());

		cut.setRequestLimit(7);
		assertEquals("Request limit should not have changed", 5, cut.getRequestLimit());
	}

	@Test
	public void testGetFreeWorkTime() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		Piece pieceMock = createMock(Piece.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		expect(fileSetMock.getBlockSize()).andStubReturn(15);
		socketMock.enqueueMessage(isA(MessageRequest.class));
		expect(pieceMock.getIndex()).andReturn(0);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		assertEquals("Initial free work time incorrect", 1, cut.getFreeWorkTime());

		cut.addBlockRequest(pieceMock, 0, 15, PeerDirection.Download);

		assertEquals("Work time should have been affected by the download job.", 0, cut.getFreeWorkTime());

		cut.setRequestLimit(5);

		assertEquals("Work time should have been affected by request limit.", 4, cut.getFreeWorkTime());
	}

	@Test
	public void testGettersForFinalFields() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		assertTrue("Socket reference has changed", socketMock == cut.getBitTorrentSocket());
		assertTrue("Torrent reference has changed", torrentMock == cut.getTorrent());
	}

	@Test
	public void testToString() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		assertTrue("Incorrect toString start", cut.toString().startsWith("Peer["));
	}

	@Test
	public void testHasExtensionIllegalIndexOutOfBounds() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("index");

		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(new byte[]{(byte) 0x01, 0x2, 0, 0, 0, 0, 0, 0 })
				.build();

		cut.hasExtension(8, 0);
	}

	@Test
	public void testCheckDisconnect() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		expect(socketMock.getLastActivity()).andAnswer(() -> LocalDateTime.now().minus(40, ChronoUnit.SECONDS));
		socketMock.enqueueMessage(isA(MessageKeepAlive.class));

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.checkDisconnect();

		verifyAll();
	}

	@Test
	public void testOnTorrentPhaseChange() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andReturn(new byte[1]).times(2);
		expect(torrentMock.isDownloadingMetadata()).andReturn(false);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.onTorrentPhaseChange();

		verifyAll();
	}

	@Test
	public void testOnTorrentPhaseChangeDownloadingMetadata() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		expect(torrentMock.isDownloadingMetadata()).andReturn(true);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.onTorrentPhaseChange();

		verifyAll();
	}

	@Test
	public void testGetLastActivity() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		LocalDateTime time = LocalDateTime.now();
		expect(socketMock.getLastActivity()).andReturn(time);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		assertEquals("Incorrect date returned", time, cut.getLastActivity());

		verifyAll();
	}

	@Test
	public void testCheckDisconnectIdle() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		expect(socketMock.getLastActivity()).andAnswer(() -> LocalDateTime.now().minus(10, ChronoUnit.SECONDS));

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.checkDisconnect();

		verifyAll();
	}

	@Test
	public void testCheckDisconnectFiveStrikes() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);
		socketMock.close();

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.addStrike(5);
		cut.checkDisconnect();

		verifyAll();
	}

	@Test
	public void testHasExtensionIllegalIndexBelowZero() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("index");

		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(new byte[]{(byte) 0x01, 0x2, 0, 0, 0, 0, 0, 0 })
				.build();

		cut.hasExtension(-1, 0);
	}

	@Test
	public void testHasExtension() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(new byte[]{(byte) 0x01, 0x2, 0, 0, 0, 0, 0, 0 })
				.build();

		assertFalse("Extension should not have reported true", cut.hasExtension(0, 0));
		assertTrue("Extension should have reported true", cut.hasExtension(0, 1));
		assertTrue("Extension should have reported true", cut.hasExtension(1, 2));
	}

	@Test
	public void testSetGetClientName() {
		Torrent torrentMock = createMock(Torrent.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andStubReturn(fileSetMock);
		expect(fileSetMock.getBitfieldBytes()).andStubReturn(new byte[1]);

		replayAll();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		assertNotNull("Client name should be set based on peer ID during construction", cut.getClientName());

		final String clientName = "JavaTorrent 0.6.0";
		cut.setClientName(clientName);

		assertEquals("Client name should have equalled the value set", clientName, cut.getClientName());
	}
}
