package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBitfield;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.disk.DiskJobCheckHash;
import org.johnnei.javatorrent.disk.DiskJobWriteBlock;
import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.AdditionalMatchers.aryEq;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link Torrent}
 */
public class TorrentTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testSetGetPieceSelector() {
		Torrent cut = DummyEntity.createUniqueTorrent();

		IPieceSelector selector = (p) -> Optional.empty();

		assertNotEquals("Selector should not be equal before setting", selector, cut.getPieceSelector());

		cut.setPieceSelector(selector);

		assertEquals("Selector should not be equal before setting", selector, cut.getPieceSelector());
	}

	@Test
	public void testEquality() {
		Torrent base = DummyEntity.createUniqueTorrent();
		Torrent notEqual = DummyEntity.createUniqueTorrent(base);
		Torrent equal = new Torrent.Builder()
				.setName("Torrent")
				.setMetadata(new Metadata.Builder().setHash(Arrays.copyOf(base.getMetadata().getHash(), 20)).build())
				.build();

		TestUtils.assertEqualityMethods(base, equal, notEqual);
	}

	@Test
	public void testAddDiskJob() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Piece pieceMock = mock(Piece.class);

		Torrent cut = new Torrent.Builder()
				.setName("Add Disk Job Test")
				.setMetadata(DummyEntity.createMetadata())
				.setTorrentClient(torrentClientMock)
				.build();

		cut.addDiskJob(new DiskJobCheckHash(pieceMock, job -> {}));
	}

	@Test
	public void testCheckForProgress() throws Exception {
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		Piece pieceMockOne = mock(Piece.class);
		Piece pieceMockTwo = mock(Piece.class);
		Piece pieceMockThree = mock(Piece.class);

		when(fileSetMock.getNeededPieces()).thenReturn(Stream.of(pieceMockOne, pieceMockTwo, pieceMockThree));

		when(pieceMockOne.checkHash()).thenReturn(true);
		when(pieceMockOne.getIndex()).thenReturn(0);
		when(pieceMockTwo.checkHash()).thenReturn(false);
		when(pieceMockThree.checkHash()).thenThrow(new IOException("Test Check For Progress IO Exception"));
		when(pieceMockThree.getIndex()).thenReturn(2);

		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		Peer peerMock = mock(Peer.class);

		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		socketMock.setPassedHandshake();
		socketMock.enqueueMessage(isA(MessageHave.class));

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getModules()).thenReturn(Collections.emptyList());

		Torrent cut = new Torrent.Builder()
				.setTorrentClient(torrentClientMock)
				.setName("Check for progress test")
				.setMetadata(DummyEntity.createMetadata())
				.build();
		cut.setFileSet(fileSetMock);

		cut.addPeer(peerMock);
		cut.checkProgress();

		verify(fileSetMock).setHavingPiece(eq(0));
	}

	@Test
	public void testSeederLeecherCount() throws Exception {
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		when(fileSetMock.getPieceCount()).thenReturn(5);

		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		Peer peerMock = mock(Peer.class);

		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		when(peerMock.countHavePieces()).thenReturn(5);

		BitTorrentSocket socketMockTwo = mock(BitTorrentSocket.class);
		Peer peerMockTwo = mock(Peer.class);

		when(peerMockTwo.getBitTorrentSocket()).thenReturn(socketMockTwo);
		when(peerMockTwo.countHavePieces()).thenReturn(3);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getModules()).thenReturn(Collections.emptyList());

		Torrent cut = new Torrent.Builder()
				.setTorrentClient(torrentClientMock)
				.setMetadata(DummyEntity.createMetadata())
				.setName("Upload/Download rates test")
				.build();

		cut.addPeer(peerMock);
		cut.addPeer(peerMockTwo);

		assertEquals("All peers should be leechers at this point.", 2, cut.getLeecherCount());
		assertEquals("We don't know if any peers are seeders yet.", 0, cut.getSeedCount());

		cut.setFileSet(fileSetMock);

		assertEquals("All peers should be leechers at this point.", 1, cut.getLeecherCount());
		assertEquals("We don't know if any peers are seeders yet.", 1, cut.getSeedCount());
	}

	@Test
	public void testUploadDownloadRates() throws Exception {
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		Peer peerMock = mock(Peer.class);

		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		when(socketMock.getDownloadRate()).thenReturn(5);
		when(socketMock.getUploadRate()).thenReturn(3);
		socketMock.pollRates();
		socketMock.setPassedHandshake();

		BitTorrentSocket socketMockTwo = mock(BitTorrentSocket.class);
		Peer peerMockTwo = mock(Peer.class);

		when(peerMockTwo.getBitTorrentSocket()).thenReturn(socketMockTwo);
		when(socketMockTwo.getDownloadRate()).thenReturn(10);
		when(socketMockTwo.getUploadRate()).thenReturn(13);
		socketMockTwo.pollRates();
		socketMockTwo.setPassedHandshake();

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getModules()).thenReturn(Collections.emptyList());

		Torrent cut = new Torrent.Builder()
				.setTorrentClient(torrentClientMock)
				.setName("Upload/Download rates test")
				.setMetadata(DummyEntity.createMetadata())
				.build();

		cut.addPeer(peerMock);
		cut.addPeer(peerMockTwo);
		cut.pollRates();

		assertEquals("Download speed aren't added up correctly", 15, cut.getDownloadRate());
		assertEquals("Upload speed aren't added up correctly", 16, cut.getUploadRate());
	}

	@Test
	public void testOnReceivedBlockChechHashMismatch() throws Exception {
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentClient torrentClient = mock(TorrentClient.class);
		Piece pieceMock = mock(Piece.class);

		ArgumentCaptor<IDiskJob> writeJobCapture = ArgumentCaptor.forClass(IDiskJob.class);
		ArgumentCaptor<IDiskJob> checkHashCapture = ArgumentCaptor.forClass(IDiskJob.class);

		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getPiece(eq(0))).thenReturn(pieceMock);
		when(pieceMock.getBlockSize(eq(1))).thenReturn(15);
		pieceMock.onHashMismatch();

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		when(pieceMock.checkHash()).thenReturn(false);
		when(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).thenReturn(2);
		when(pieceMock.getBlockCount()).thenReturn(2);

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setMetadata(DummyEntity.createMetadata())
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.onReceivedBlock(fileSetMock, 0, 15, new byte[15]);

		verify(torrentClient).addDiskJob(writeJobCapture.capture());
		verify(torrentClient).addDiskJob(checkHashCapture.capture());

		writeJobCapture.getValue().process();
		checkHashCapture.getValue().process();
	}

	@Test
	public void testOnReceivedBlockChechHash() throws Exception {
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentClient torrentClient = mock(TorrentClient.class);
		Piece pieceMock = mock(Piece.class);

		ArgumentCaptor<DiskJobWriteBlock> writeJobCapture = ArgumentCaptor.forClass(DiskJobWriteBlock.class);
		ArgumentCaptor<DiskJobCheckHash> checkHashCapture = ArgumentCaptor.forClass(DiskJobCheckHash.class);

		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getPiece(eq(0))).thenReturn(pieceMock);
		when(pieceMock.getBlockSize(eq(1))).thenReturn(15);
		when(pieceMock.getIndex()).thenReturn(1);
		when(pieceMock.getSize()).thenReturn(15);
		fileSetMock.setHavingPiece(eq(1));
		when(torrentClient.getModules()).thenReturn(Collections.emptyList());

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		when(pieceMock.checkHash()).thenReturn(true);
		when(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).thenReturn(2);
		when(pieceMock.getBlockCount()).thenReturn(2);
		when(pieceMock.getFileSet()).thenReturn(fileSetMock);

		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		Peer peerMock = mock(Peer.class);

		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		socketMock.setPassedHandshake();
		when(fileSetMock.countCompletedPieces()).thenReturn(0);
		socketMock.enqueueMessage(isA(MessageHave.class));

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setMetadata(DummyEntity.createMetadata())
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.addPeer(peerMock);
		cut.onReceivedBlock(fileSetMock, 0, 15, new byte[15]);

		assertEquals("Incorrect downloaded bytes, nothing is completed yet.", 0, cut.getDownloadedBytes());

		verify(torrentClient).addDiskJob(writeJobCapture.capture());
		writeJobCapture.getValue().process();
		verify(torrentClient, times(2)).addDiskJob(checkHashCapture.capture());
		checkHashCapture.getValue().process();

		assertEquals("Incorrect downloaded bytes, piece size should have been added.", 15, cut.getDownloadedBytes());
	}

	@Test
	public void testOnReceivedBlockIncorrectSize() throws Exception {
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentClient torrentClient = mock(TorrentClient.class);
		Piece pieceMock = mock(Piece.class);

		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getPiece(eq(0))).thenReturn(pieceMock);
		when(pieceMock.getBlockSize(eq(1))).thenReturn(10);

		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Needed));

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setMetadata(DummyEntity.createMetadata())
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.onReceivedBlock(fileSetMock, 0, 15, new byte[15]);
	}

	@Test
	public void testOnReceivedBlockPieceNotDone() throws Exception {
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentClient torrentClient = mock(TorrentClient.class);
		Piece pieceMock = mock(Piece.class);

		ArgumentCaptor<IDiskJob> writeJobCapture = ArgumentCaptor.forClass(IDiskJob.class);

		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getPiece(eq(0))).thenReturn(pieceMock);
		when(pieceMock.getBlockSize(eq(1))).thenReturn(15);

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		when(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).thenReturn(1);
		when(pieceMock.getBlockCount()).thenReturn(2);

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setMetadata(DummyEntity.createMetadata())
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.onReceivedBlock(fileSetMock, 0, 15, new byte[15]);

		verify(torrentClient).addDiskJob(writeJobCapture.capture());
		writeJobCapture.getValue().process();
	}

	@Test
	public void testOnReceivedBlockChechHashDownloadingMetadata() throws Exception {
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentClient torrentClient = mock(TorrentClient.class);
		Piece pieceMock = mock(Piece.class);

		ArgumentCaptor<IDiskJob> writeJobCapture = ArgumentCaptor.forClass(IDiskJob.class);
		ArgumentCaptor<IDiskJob> checkHashCapture = ArgumentCaptor.forClass(IDiskJob.class);

		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getPiece(eq(0))).thenReturn(pieceMock);
		when(pieceMock.getBlockSize(eq(1))).thenReturn(15);
		when(pieceMock.getIndex()).thenReturn(0);

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		when(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).thenReturn(2);
		when(pieceMock.getBlockCount()).thenReturn(2);
		when(pieceMock.checkHash()).thenReturn(true);

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setMetadata(DummyEntity.createMetadata())
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.onReceivedBlock(fileSetMock, 0, 15, new byte[15]);

		verify(torrentClient).addDiskJob(writeJobCapture.capture());
		verify(torrentClient).addDiskJob(checkHashCapture.capture());
		writeJobCapture.getValue().process();
		checkHashCapture.getValue().process();
	}

	@Test
	public void testOnReceivedBlock() throws Exception {
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentClient torrentClient = mock(TorrentClient.class);
		Piece pieceMock = mock(Piece.class);

		ArgumentCaptor<IDiskJob> writeJobCapture = ArgumentCaptor.forClass(IDiskJob.class);
		ArgumentCaptor<IDiskJob> checkHashCapture = ArgumentCaptor.forClass(IDiskJob.class);

		when(fileSetMock.getBlockSize()).thenReturn(15);
		when(fileSetMock.getPiece(eq(0))).thenReturn(pieceMock);
		when(pieceMock.getBlockSize(eq(1))).thenReturn(15);

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		when(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).thenReturn(2);
		when(pieceMock.getBlockCount()).thenReturn(2);

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setMetadata(DummyEntity.createMetadata())
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.onReceivedBlock(fileSetMock, 0, 15, new byte[15]);

		verify(torrentClient).addDiskJob(writeJobCapture.capture());
		verify(torrentClient).addDiskJob(checkHashCapture.capture());
		writeJobCapture.getValue().process();
	}

	@Test
	public void testToString() {
		Torrent cut = DummyEntity.createUniqueTorrent();

		assertTrue("Incorrect toString start", cut.toString().startsWith("Torrent["));
	}

	@Test
	public void testAddRemovePeer() throws Exception {
		byte[] peerId = DummyEntity.createUniquePeerId();
		byte[] peerIdTwo = DummyEntity.createUniquePeerId(peerId);

		BitTorrentSocket socketMockOne = mock(BitTorrentSocket.class);
		socketMockOne.setPassedHandshake();

		BitTorrentSocket socketMockTwo = mock(BitTorrentSocket.class);
		socketMockTwo.setPassedHandshake();

		BitTorrentSocket socketMockThree = mock(BitTorrentSocket.class);
		socketMockThree.close();

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getModules()).thenReturn(Collections.emptyList());

		Torrent cut = DummyEntity.createUniqueTorrent(torrentClientMock);

		Peer peerOne = new Peer.Builder()
				.setSocket(socketMockOne)
				.setTorrent(cut)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		Peer peerThree = new Peer.Builder()
				.setSocket(socketMockThree)
				.setTorrent(cut)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		Peer peerTwo = new Peer.Builder()
				.setSocket(socketMockTwo)
				.setTorrent(cut)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerIdTwo)
				.build();

		assertEquals("Peer list should be empty", 0, cut.getPeers().size());

		cut.addPeer(peerOne);

		assertEquals("Peer one should have been added to the list.", 1, cut.getPeers().size());
		assertTrue("Peer one should have been added to the list.", cut.getPeers().contains(peerOne));

		cut.addPeer(peerThree);

		assertEquals("Peer three should not have been added to the list twice.", 1, cut.getPeers().size());
		assertFalse("Peer three should not be present in the list.", cut.getPeers().stream().anyMatch(p -> p == peerThree));

		cut.addPeer(peerTwo);

		assertEquals("Peer two should have been added to the list.", 2, cut.getPeers().size());
		assertTrue("Peer two should have been added to the list.", cut.getPeers().contains(peerTwo));

		cut.removePeer(peerTwo);

		assertEquals("Peer two should have been removed from the list.", 1, cut.getPeers().size());
		assertFalse("Peer two should have been removed to the list.", cut.getPeers().contains(peerTwo));

		cut.removePeer(peerTwo);

		assertEquals("Removal of peer two twice should not affect the list on the second attempt.", 1, cut.getPeers().size());
	}

	@Test
	public void testSendHaveMessagesNoMessages() throws Exception {
		byte[] peerId = DummyEntity.createUniquePeerId();

		BitTorrentSocket socketMockOne = mock(BitTorrentSocket.class);
		socketMockOne.setPassedHandshake();

		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		when(fileSetMock.countCompletedPieces()).thenReturn(0);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[0]);

		IModule moduleMock = mock(IModule.class);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getModules()).thenReturn(Collections.singletonList(moduleMock));

		Torrent cut = DummyEntity.createUniqueTorrent(torrentClientMock);
		cut.setFileSet(fileSetMock);

		Peer peer = new Peer.Builder()
				.setSocket(socketMockOne)
				.setTorrent(cut)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		cut.addPeer(peer);
	}

	@Test
	public void testSendHaveMessagesSendBitfield() throws Exception {
		byte[] peerId = DummyEntity.createUniquePeerId();

		BitTorrentSocket socketMockOne = mock(BitTorrentSocket.class);
		socketMockOne.setPassedHandshake();
		socketMockOne.enqueueMessage(isA(MessageBitfield.class));

		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		when(fileSetMock.countCompletedPieces()).thenReturn(7);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[2]);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getModules()).thenReturn(Collections.emptyList());

		Torrent cut = DummyEntity.createUniqueTorrent(torrentClientMock);
		cut.setFileSet(fileSetMock);

		Peer peer = new Peer.Builder()
				.setSocket(socketMockOne)
				.setTorrent(cut)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		cut.addPeer(peer);
	}

	@Test
	public void testSendHaveMessagesSendHaveMessages() throws Exception {
		byte[] peerId = DummyEntity.createUniquePeerId();

		ArgumentCaptor<MessageHave> haveCapture = ArgumentCaptor.forClass(MessageHave.class);

		BitTorrentSocket socketMockOne = mock(BitTorrentSocket.class);
		socketMockOne.setPassedHandshake();

		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		when(fileSetMock.countCompletedPieces()).thenReturn(2);
		when(fileSetMock.getBitfieldBytes()).thenReturn(new byte[12]);
		when(fileSetMock.getPieceCount()).thenReturn(96);
		when(fileSetMock.hasPiece(anyInt())).thenReturn(false);
		when(fileSetMock.hasPiece(0)).thenReturn(true);
		when(fileSetMock.hasPiece(5)).thenReturn(true);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getModules()).thenReturn(Collections.emptyList());

		Torrent cut = DummyEntity.createUniqueTorrent(torrentClientMock);
		cut.setFileSet(fileSetMock);

		Peer peer = new Peer.Builder()
				.setSocket(socketMockOne)
				.setTorrent(cut)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		cut.addPeer(peer);

		verify(socketMockOne, times(2)).enqueueMessage(haveCapture.capture());

		List<Integer> expectedPieces = new ArrayList<>();
		expectedPieces.add(0);
		expectedPieces.add(5);
		for (MessageHave message : haveCapture.getAllValues()) {
			expectedPieces.remove(Whitebox.<Integer>getInternalState(message, "pieceIndex"));
		}

		assertEquals("All pieces marked as have should have been send as have message", 0, expectedPieces.size());
	}

	@Test
	public void testBuilderCanDownload() {
		Torrent.Builder builder = new Torrent.Builder();

		assertFalse("Torrent should not be downloadable yet", builder.canDownload());

		builder.setName("Can download");

		assertFalse("Torrent should not be downloadable yet", builder.canDownload());

		builder.setMetadata(mock(Metadata.class));

		assertTrue("Torrent should be downloadable", builder.canDownload());
	}

	@Test
	public void testGetDisplayName() {
		Torrent cut = new Torrent.Builder()
				.setName("Test")
				.setMetadata(DummyEntity.createMetadata())
				.build();

		assertEquals("Incorrect display name", "Test", cut.getDisplayName());
	}

	@Test
	public void testGetUploadedBytes() {
		Torrent cut = new Torrent.Builder()
				.setName("Test")
				.setMetadata(DummyEntity.createMetadata())
				.build();

		assertEquals("Incorrect amount of uploaded bytes, nothing is uploaded", 0, cut.getUploadedBytes());
		cut.addUploadedBytes(15);
		assertEquals("Incorrect amount of uploaded bytes, data has been uploaded", 15, cut.getUploadedBytes());
	}

	@Test
	public void testIsDownloadingMetadata() {
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		Metadata metadataMock = mock(Metadata.class);
		AbstractFileSet metadataFileSetMock = mock(AbstractFileSet.class);

		when(metadataMock.getName()).thenReturn("Mocked Metadata");
		when(metadataMock.getFileSet()).thenReturn(Optional.of(metadataFileSetMock));

		when(metadataFileSetMock.isDone()).thenReturn(false);
		when(metadataFileSetMock.isDone()).thenReturn(true);

		Torrent cut = new Torrent.Builder()
				.setName("Test")
				.setMetadata(metadataMock)
				.build();

		assertTrue("Metadata should have returned that it is not done yet", cut.isDownloadingMetadata());
		assertTrue("Metadata should have returned that it is done, but the torrent fileset has not been set yet.", cut.isDownloadingMetadata());

		cut.setFileSet(fileSetMock);

		assertFalse("Metadata should have returned that it is done", cut.isDownloadingMetadata());
	}
}