package org.johnnei.javatorrent.torrent.peer;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBlock;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageChoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageInterested;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageKeepAlive;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUnchoke;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageUninterested;
import org.johnnei.javatorrent.disk.DiskJobReadBlock;
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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.isNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PeerTest {

	@Test
	public void testBuilderSetExtensionBytesIncorrectLength() {
		Exception e = assertThrows(IllegalArgumentException.class, () -> new Peer.Builder().setExtensionBytes(new byte[9]));
		assertThat(e.getMessage(), containsString("Extension bytes"));
	}

	@Test
	public void testBuilderSetIdIncorrectLength() {
		Exception e = assertThrows(IllegalArgumentException.class, () -> new Peer.Builder().setId(new byte[9]));
		assertThat(e.getMessage(), containsString("Id bytes"));
	}

	@Test
	public void testAddModuleInfo() {
		Peer peer = DummyEntity.createPeer();

		Object o = new Object();
		peer.addModuleInfo(o);

		assertEquals(o, assertPresent("Module was registered.", peer.getModuleInfo(Object.class)), "Returned object is not equal to inserted");
	}

	@Test
	public void testAddModuleInfoDuplicate() {
		Peer peer = DummyEntity.createPeer();

		Object o = new Object();
		Object o2 = new Object();
		peer.addModuleInfo(o);
		assertThrows(IllegalStateException.class, () -> peer.addModuleInfo(o2));
	}

	@Test
	public void testAddModuleInfoNoElement() {
		Peer peer = DummyEntity.createPeer();

		assertNotPresent("Module was not registered.", peer.getModuleInfo(Object.class));
	}

	@Test
	public void testDownloadInterested() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.setMetadata(DummyEntity.createMetadata())
				.build();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		peer.setInterested(PeerDirection.Download, true);
		verify(socketMock).enqueueMessage(isA(MessageInterested.class));
		assertTrue(peer.isInterested(PeerDirection.Download), "Incorrect interested state");
	}

	@Test
	public void testDownloadUninterested() {
		Torrent torrentMock = new Torrent.Builder()
				.setName("StubTorrent")
				.setMetadata(DummyEntity.createMetadata())
				.build();
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		Peer peer = DummyEntity.createPeer(socketMock, torrentMock);

		peer.setInterested(PeerDirection.Download, false);
		verify(socketMock).enqueueMessage(isA(MessageUninterested.class));
		assertFalse(peer.isInterested(PeerDirection.Download), "Incorrect interested state");
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
		assertTrue(peer.isInterested(PeerDirection.Upload), "Incorrect interested state");
		peer.setInterested(PeerDirection.Upload, false);
		assertFalse(peer.isInterested(PeerDirection.Upload), "Incorrect interested state");
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
		assertTrue(peer.isChoked(PeerDirection.Download), "Incorrect choked state");
		peer.setChoked(PeerDirection.Download, false);
		assertFalse(peer.isChoked(PeerDirection.Download), "Incorrect choked state");
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
		assertTrue(peer.isChoked(PeerDirection.Upload), "Incorrect choked state");
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
		assertFalse(peer.isChoked(PeerDirection.Upload), "Incorrect choked state");
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

		assertEquals(1, peer.getWorkQueueSize(PeerDirection.Download), "Working queue should have increased");
		verify(socketMock).enqueueMessage(any());
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

		assertEquals(1, peer.getWorkQueueSize(PeerDirection.Upload), "Working queue should have increased");
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
		assertEquals(2, peer.getWorkQueueSize(PeerDirection.Download), "Working queue should have two items");

		peer.cancelBlockRequest(pieceMock, 15, 15, PeerDirection.Download);

		assertEquals(1, peer.getWorkQueueSize(PeerDirection.Download), "Working queue should have one item anymore");
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
		assertEquals(2, peer.getWorkQueueSize(PeerDirection.Upload), "Working queue should have two items");

		peer.cancelBlockRequest(pieceMock, 15, 15, PeerDirection.Upload);

		assertEquals(1, peer.getWorkQueueSize(PeerDirection.Upload), "Working queue should have one item anymore");
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
		assertEquals(2, peer.getWorkQueueSize(PeerDirection.Download), "Working queue should have two items");

		peer.onReceivedBlock(pieceMock, 15);

		assertEquals(1, peer.getWorkQueueSize(PeerDirection.Download), "Working queue should have one item anymore");
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

		verify(torrentMock).addDiskJob(isNotNull());
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

		assertEquals(2, cut.getWorkQueueSize(PeerDirection.Download), "Working queue should have two items");

		cut.discardAllBlockRequests();
		assertEquals(0, cut.getWorkQueueSize(PeerDirection.Download), "Working queue should have two items");
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

		assertFalse(cut.hasPiece(0), "Piece 0 should not be available yet");
		assertEquals(0, cut.countHavePieces(), "No pieces should be completed yet");

		cut.setHavingPiece(0);

		assertTrue(cut.hasPiece(0), "Piece 0 should be available");
		assertEquals(1, cut.countHavePieces(), "One pieces should be completed");
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

		assertEquals(1, cut.getRequestLimit(), "Initial request limit should have been 1");

		cut.setRequestLimit(7);
		assertEquals(7, cut.getRequestLimit(), "Request limit should have changed");
		cut.setRequestLimit(-1);
		assertEquals(7, cut.getRequestLimit(), "Request limit should not have changed");

		cut.setAbsoluteRequestLimit(5);
		assertEquals(5, cut.getRequestLimit(), "Request limit should have been limited");

		cut.setRequestLimit(7);
		assertEquals(5, cut.getRequestLimit(), "Request limit should not have changed");
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

		assertEquals(1, cut.getFreeWorkTime(), "Initial free work time incorrect");

		cut.addBlockRequest(pieceMock, 0, 15, PeerDirection.Download);

		assertEquals(0, cut.getFreeWorkTime(), "Work time should have been affected by the download job.");
		verify(socketMock).enqueueMessage(any());
		verify(requestMock).createRequestFor(cut, pieceMock, 0, 15);

		cut.setRequestLimit(5);

		assertEquals(4, cut.getFreeWorkTime(), "Work time should have been affected by request limit.");
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

		assertTrue(socketMock == cut.getBitTorrentSocket(), "Socket reference has changed");
		assertTrue(torrentMock == cut.getTorrent(), "Torrent reference has changed");
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

		assertTrue(cut.toString().startsWith("Peer["), "Incorrect toString start");
	}

	@Test
	public void testHasExtensionIllegalIndexOutOfBounds() {
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

		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.hasExtension(8, 0));
		assertThat(e.getMessage(), containsString("index"));
	}

	@Test
	public void testCheckDisconnect() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(socketMock.getLastActivity()).thenAnswer(invocation -> LocalDateTime.now().minus(40, ChronoUnit.SECONDS));
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

		assertEquals(time, cut.getLastActivity(), "Incorrect date returned");
	}

	@Test
	public void testCheckDisconnectIdle() {
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[1]);
		when(socketMock.getLastActivity()).thenAnswer(inv -> LocalDateTime.now().minus(10, ChronoUnit.SECONDS));

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

		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.hasExtension(-1, 0));
		assertThat(e.getMessage(), containsString("index"));
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

		assertFalse(cut.hasExtension(0, 0), "Extension should not have reported true");
		assertTrue(cut.hasExtension(0, 1), "Extension should have reported true");
		assertTrue(cut.hasExtension(1, 2), "Extension should have reported true");
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

		assertNotNull(cut.getClientName(), "Client name should be set based on peer ID during construction");

		final String clientName = "JavaTorrent 0.6.0";
		cut.setClientName(clientName);

		assertEquals(clientName, cut.getClientName(), "Client name should have equalled the value set");
	}

	@Test
	public void testCancelBlockRequestNonCancellable() {
		Piece pieceMock = mock(Piece.class);
		AbstractFileSet fileSetMock = mock(AbstractFileSet.class);
		IFileSetRequestFactory requestFactoryMock = mock(IFileSetRequestFactory.class);

		when(pieceMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getRequestFactory()).thenReturn(requestFactoryMock);
		when(requestFactoryMock.supportsCancellation()).thenReturn(false);

		Peer cut = DummyEntity.createPeer();
		Exception e = assertThrows(IllegalArgumentException.class, () -> cut.cancelBlockRequest(pieceMock, 0, 50, PeerDirection.Download));
		assertThat(e.getMessage(), containsString("cancel"));
	}
}
