package org.johnnei.javatorrent.torrent.peer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUninterested;
import org.johnnei.javatorrent.disk.DiskJobReadBlock;
import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.internal.torrent.TorrentFileSetRequestFactory;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.IFileSetRequestFactory;
import org.johnnei.javatorrent.torrent.files.Piece;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PeerTest {

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
				.setMetadata(DummyEntity.createMetadata())
				.build();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		socketMock.enqueueMessage(isA(MessageInterested.class));

		peer.setInterested(PeerDirection.Download, true);
		assertTrue("Incorrect interested state", peer.isInterested(PeerDirection.Download));
	}

	@Test
	public void testDownloadUninterested() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.setMetadata(DummyEntity.createMetadata())
				.build();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		socketMock.enqueueMessage(isA(MessageUninterested.class));

		peer.setInterested(PeerDirection.Download, false);
		assertFalse("Incorrect interested state", peer.isInterested(PeerDirection.Download));
	}

	@Test
	public void testUploadInterested() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.setMetadata(DummyEntity.createMetadata())
				.build();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		peer.setInterested(PeerDirection.Upload, true);
		assertTrue("Incorrect interested state", peer.isInterested(PeerDirection.Upload));
		peer.setInterested(PeerDirection.Upload, false);
		assertFalse("Incorrect interested state", peer.isInterested(PeerDirection.Upload));
	}

	@Test
	public void testDownloadChoke() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.setMetadata(DummyEntity.createMetadata())
				.build();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		peer.setChoked(PeerDirection.Download, true);
		assertTrue("Incorrect choked state", peer.isChoked(PeerDirection.Download));
		peer.setChoked(PeerDirection.Download, false);
		assertFalse("Incorrect choked state", peer.isChoked(PeerDirection.Download));
	}

	@Test
	public void testUploadChoke() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.setMetadata(DummyEntity.createMetadata())
				.build();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		socketMock.enqueueMessage(isA(MessageChoke.class));

		peer.setChoked(PeerDirection.Upload, true);
		assertTrue("Incorrect choked state", peer.isChoked(PeerDirection.Upload));
	}

	@Test
	public void testDownloadUnchoke() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.setMetadata(DummyEntity.createMetadata())
				.build();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		socketMock.enqueueMessage(isA(MessageUnchoke.class));

		peer.setChoked(PeerDirection.Upload, false);
		assertFalse("Incorrect choked state", peer.isChoked(PeerDirection.Upload));
	}

	@Test
	public void testEquality() {
		Torrent torrent = DummyEntity.createUniqueTorrent();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

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
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		Piece pieceMock = mock(Piece.class);

		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(fileSetMock.getRequestFactory()).thenReturn(mock(TorrentFileSetRequestFactory.class));
		when(pieceMock.getIndex()).thenReturn(0);
		when(pieceMock.getFileSet()).thenReturn(fileSetMock);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Download);

		assertEquals("Working queue should have increased", 1, peer.getWorkQueueSize(PeerDirection.Download));
		verify(socketMock).enqueueMessage(anyObject());
	}

	@Test
	public void testAddBlockRequestUpload() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		Piece pieceMock = mock(Piece.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Upload);

		assertEquals("Working queue should have increased", 1, peer.getWorkQueueSize(PeerDirection.Upload));
	}

	@Test
	public void testCancelBlockRequestDownload() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentFileSetRequestFactory requestFactoryMock = mock(TorrentFileSetRequestFactory.class);
		Piece pieceMock = mock(Piece.class);

		when(requestFactoryMock.supportsCancellation()).thenReturn(true);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(fileSetMock.getPiece(0)).thenReturn(pieceMock);
		when(pieceMock.getIndex()).thenReturn(0);
		when(pieceMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getRequestFactory()).thenReturn(requestFactoryMock);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Download);
		peer.addBlockRequest(pieceMock, 30, 15, PeerDirection.Download);
		assertEquals("Working queue should have two items", 2, peer.getWorkQueueSize(PeerDirection.Download));

		peer.cancelBlockRequest(pieceMock, 15, 15, PeerDirection.Download);

		assertEquals("Working queue should have one item anymore", 1, peer.getWorkQueueSize(PeerDirection.Download));
		verify(socketMock, times(3)).enqueueMessage(any());
	}

	@Test
	public void testCancelBlockRequestUpload() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentFileSetRequestFactory requestFactoryMock = mock(TorrentFileSetRequestFactory.class);
		Piece pieceMock = mock(Piece.class);

		when(requestFactoryMock.supportsCancellation()).thenReturn(true);
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(fileSetMock.getPiece(0)).thenReturn(pieceMock);
		when(fileSetMock.getRequestFactory()).thenReturn(requestFactoryMock);
		when(pieceMock.getFileSet()).thenReturn(fileSetMock);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Upload);
		peer.addBlockRequest(pieceMock, 30, 15, PeerDirection.Upload);
		assertEquals("Working queue should have two items", 2, peer.getWorkQueueSize(PeerDirection.Upload));

		peer.cancelBlockRequest(pieceMock, 15, 15, PeerDirection.Upload);

		assertEquals("Working queue should have one item anymore", 1, peer.getWorkQueueSize(PeerDirection.Upload));
	}

	@Test
	public void testOnReceivedBlock() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentFileSetRequestFactory requestFactoryMock = mock(TorrentFileSetRequestFactory.class);
		Piece pieceMock = mock(Piece.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(fileSetMock.getPiece(eq(0))).thenReturn(pieceMock);
		when(pieceMock.getBlockSize(eq(1))).thenReturn(15);
		when(pieceMock.getIndex()).thenReturn(0);
		when(pieceMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getRequestFactory()).thenReturn(requestFactoryMock);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);
		peer.addBlockRequest(pieceMock, 15, 15, PeerDirection.Download);
		peer.addBlockRequest(pieceMock, 30, 15, PeerDirection.Download);
		assertEquals("Working queue should have two items", 2, peer.getWorkQueueSize(PeerDirection.Download));

		peer.onReceivedBlock(pieceMock, 15);

		assertEquals("Working queue should have one item anymore", 1, peer.getWorkQueueSize(PeerDirection.Download));
		verify(requestFactoryMock).createRequestFor(peer, pieceMock, 15, 15);
		verify(requestFactoryMock).createRequestFor(peer, pieceMock, 30, 15);
		verify(socketMock, times(2)).enqueueMessage(any());
	}

	@Test
	public void testQueueNextPieceForSendingNoPendingJobs() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.queueNextPieceForSending();
	}

	@Test
	public void testQueueNextPieceForSendingSingleRequestOnly() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		Piece pieceMock = mock(Piece.class);
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.addBlockRequest(pieceMock, 0, 15, PeerDirection.Upload);
		cut.queueNextPieceForSending();
		cut.queueNextPieceForSending();

		verify(torrentMock).addDiskJob((IDiskJob) isNotNull());
	}

	@Test
	public void testQueueNextPieceForSending() throws Exception {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		Piece pieceMock = mock(Piece.class);

		socketMock.enqueueMessage(isA(MessageBlock.class));
		torrentMock.addUploadedBytes(15);
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(pieceMock.getIndex()).thenReturn(0);
		when(pieceMock.loadPiece(eq(0), eq(15))).thenReturn(new byte[15]);

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.addBlockRequest(pieceMock, 0, 15, PeerDirection.Upload);
		cut.queueNextPieceForSending();

		ArgumentCaptor<DiskJobReadBlock> diskJobCapture = ArgumentCaptor.forClass(DiskJobReadBlock.class);
		verify(torrentMock).addDiskJob(diskJobCapture.capture());

		diskJobCapture.getValue().process();
	}

	@Test
	public void testDiscardAllBlockRequests() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		Piece pieceMock = mock(Piece.class);

		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(pieceMock.getIndex()).thenReturn(0);
		when(fileSetMock.getRequestFactory()).thenReturn(mock(TorrentFileSetRequestFactory.class));
		when(pieceMock.getFileSet()).thenReturn(fileSetMock);
		pieceMock.setBlockStatus(eq(0), eq(BlockStatus.Needed));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Needed));

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
		assertEquals("Working queue should have two items", 0, cut.getWorkQueueSize(PeerDirection.Download));
		verify(socketMock, times(2)).enqueueMessage(any());

	}

	@Test
	public void testSetHasPiece() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(torrentMock.isDownloadingMetadata()).thenReturn(false);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(fileSetMock.getBlockSize()).thenReturn(15);

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
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

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
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentFileSetRequestFactory requestMock = mock(TorrentFileSetRequestFactory.class);
		Piece pieceMock = mock(Piece.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getRequestFactory()).thenReturn(requestMock);
		when(pieceMock.getIndex()).thenReturn(0);
		when(pieceMock.getFileSet()).thenReturn(fileSetMock);

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		assertEquals("Initial free work time incorrect", 1, cut.getFreeWorkTime());

		cut.addBlockRequest(pieceMock, 0, 15, PeerDirection.Download);

		assertEquals("Work time should have been affected by the download job.", 0, cut.getFreeWorkTime());
		verify(socketMock).enqueueMessage(any());
		verify(requestMock).createRequestFor(cut, pieceMock, 0, 15);

		cut.setRequestLimit(5);

		assertEquals("Work time should have been affected by request limit.", 4, cut.getFreeWorkTime());
	}

	@Test
	public void testGettersForFinalFields() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

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
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

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

		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

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
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(socketMock.getLastActivity()).thenAnswer(new Answer<LocalDateTime>() {
			@Override
			public LocalDateTime answer(InvocationOnMock invocation) throws Throwable {
				return LocalDateTime.now().minus(40, ChronoUnit.SECONDS);
			}
		});
		socketMock.enqueueMessage(isA(MessageKeepAlive.class));

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.checkDisconnect();
	}

	@Test
	public void testOnTorrentPhaseChange() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(torrentMock.isDownloadingMetadata()).thenReturn(false);

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.onTorrentPhaseChange();
	}

	@Test
	public void testOnTorrentPhaseChangeDownloadingMetadata() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(torrentMock.isDownloadingMetadata()).thenReturn(true);

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.onTorrentPhaseChange();
	}

	@Test
	public void testGetLastActivity() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

		LocalDateTime time = LocalDateTime.now();
		when(socketMock.getLastActivity()).thenReturn(time);

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		assertEquals("Incorrect date returned", time, cut.getLastActivity());
	}

	@Test
	public void testCheckDisconnectIdle() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(socketMock.getLastActivity()).thenAnswer(new Answer<LocalDateTime>() {
			@Override
			public LocalDateTime answer(InvocationOnMock invocation) throws Throwable {
				return LocalDateTime.now().minus(10, ChronoUnit.SECONDS);
			}
		});

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.checkDisconnect();
	}

	@Test
	public void testCheckDisconnectFiveStrikes() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		socketMock.close();

		Peer cut = new Peer.Builder()
				.setTorrent(torrentMock)
				.setSocket(socketMock)
				.setId(DummyEntity.createUniquePeerId())
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.build();

		cut.addStrike(5);
		cut.checkDisconnect();
	}

	@Test
	public void testHasExtensionIllegalIndexBelowZero() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("index");

		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

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
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

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
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);

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

	@Test
	public void testCancelBlockRequestNonCancellable() {
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("cancel");

		Piece pieceMock = mock(Piece.class);
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);
		IFileSetRequestFactory requestFactoryMock = mock(IFileSetRequestFactory.class);

		when(pieceMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getRequestFactory()).thenReturn(requestFactoryMock);
		when(requestFactoryMock.supportsCancellation()).thenReturn(false);

		Peer cut = DummyEntity.createPeer();
		cut.cancelBlockRequest(pieceMock, 0, 50, PeerDirection.Download);
	}
}
