package org.johnnei.javatorrent.torrent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

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
import org.johnnei.junit.jupiter.TempFolderExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
@ExtendWith(TempFolderExtension.class)
public class TorrentTest {

	@Test
	public void testSetGetPieceSelector() {
		Torrent cut = DummyEntity.createUniqueTorrent();

		IPieceSelector selector = (p) -> Optional.empty();

		assertNotEquals(selector, cut.getPieceSelector(), "Selector should not be equal before setting");

		cut.setPieceSelector(selector);

		assertEquals(selector, cut.getPieceSelector(), "Selector should not be equal before setting");
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

		assertEquals(2, cut.getLeecherCount(), "All peers should be leechers at this point.");
		assertEquals(0, cut.getSeedCount(), "We don't know if any peers are seeders yet.");

		cut.setFileSet(fileSetMock);

		assertEquals(1, cut.getLeecherCount(), "All peers should be leechers at this point.");
		assertEquals(1, cut.getSeedCount(), "We don't know if any peers are seeders yet.");
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

		assertEquals(15, cut.getDownloadRate(), "Download speed aren't added up correctly");
		assertEquals(16, cut.getUploadRate(), "Upload speed aren't added up correctly");
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

		assertEquals(0, cut.getDownloadedBytes(), "Incorrect downloaded bytes, nothing is completed yet.");

		verify(torrentClient).addDiskJob(writeJobCapture.capture());
		writeJobCapture.getValue().process();
		verify(torrentClient, times(2)).addDiskJob(checkHashCapture.capture());
		checkHashCapture.getValue().process();

		assertEquals(15, cut.getDownloadedBytes(), "Incorrect downloaded bytes, piece size should have been added.");
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

		assertTrue(cut.toString().startsWith("Torrent["), "Incorrect toString start");
	}

	@Test
	public void testAddRemovePeer() throws Exception {
		byte[] peerId = DummyEntity.createUniquePeerId();
		byte[] peerIdTwo = DummyEntity.createUniquePeerId(peerId);

		BitTorrentSocket socketMockOne = mock(BitTorrentSocket.class);
		BitTorrentSocket socketMockTwo = mock(BitTorrentSocket.class);
		BitTorrentSocket socketMockThree = mock(BitTorrentSocket.class);

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

		assertEquals(0, cut.getPeers().size(), "Peer list should be empty");

		cut.addPeer(peerOne);

		assertEquals(1, cut.getPeers().size(), "Peer one should have been added to the list.");
		assertThat("Peer one should have been added to the list.", cut.getPeers(), hasItems(peerOne));
		verify(socketMockOne).setPassedHandshake();

		cut.addPeer(peerThree);

		assertEquals(1, cut.getPeers().size(), "Peer three should not have been added to the list as it has the same peer id as peer one.");
		assertThat("Peer three should not be present in the list.", cut.getPeers(), not(hasItems(sameInstance(peerThree))));
		verify(socketMockThree).close();

		cut.addPeer(peerTwo);

		assertThat("Peer two should have been added to the list.", cut.getPeers(), hasSize(2));
		assertThat("Peer two should have been added to the list.", cut.getPeers(), hasItem(peerTwo));
		verify(socketMockTwo).setPassedHandshake();

		cut.removePeer(peerTwo);

		assertThat("Peer two should have been removed from the list.", cut.getPeers(), hasSize(1));
		assertThat("Peer two should have been removed to the list.", cut.getPeers(), not(hasItem(peerTwo)));

		cut.removePeer(peerTwo);

		assertThat("Removal of peer two twice should not affect the list on the second attempt.", cut.getPeers(), hasSize(1));
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

		assertThat("All pieces marked as have should have been send as have message", expectedPieces, empty());
	}

	@Test
	public void testBuilderCanDownload() {
		Torrent.Builder builder = new Torrent.Builder();

		assertFalse(builder.canDownload(), "Torrent should not be downloadable yet");

		builder.setName("Can download");

		assertFalse(builder.canDownload(), "Torrent should not be downloadable yet");

		builder.setMetadata(mock(Metadata.class));

		assertTrue(builder.canDownload(), "Torrent should be downloadable");
	}

	@Test
	public void testGetDisplayName() {
		Torrent cut = new Torrent.Builder()
				.setName("Test")
				.setMetadata(DummyEntity.createMetadata())
				.build();

		assertEquals("Test", cut.getDisplayName(), "Incorrect display name");
	}

	@Test
	public void testGetUploadedBytes() {
		Torrent cut = new Torrent.Builder()
				.setName("Test")
				.setMetadata(DummyEntity.createMetadata())
				.build();

		assertEquals(0, cut.getUploadedBytes(), "Incorrect amount of uploaded bytes, nothing is uploaded");
		cut.addUploadedBytes(15);
		assertEquals(15, cut.getUploadedBytes(), "Incorrect amount of uploaded bytes, data has been uploaded");
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

		assertTrue(cut.isDownloadingMetadata(), "Metadata should have returned that it is not done yet");
		assertTrue(cut.isDownloadingMetadata(), "Metadata should have returned that it is done, but the torrent fileset has not been set yet.");

		cut.setFileSet(fileSetMock);

		assertFalse(cut.isDownloadingMetadata(), "Metadata should have returned that it is done");
	}

	@Test
	public void testBuildWithoutName() {
		Metadata metadataMock = mock(Metadata.class);
		when(metadataMock.getName()).thenReturn("magnet(ab)");

		Torrent cut = new Torrent.Builder()
				.setMetadata(metadataMock)
				.build();

		assertEquals("magnet(ab)", cut.getDisplayName(), "As no name has been supplied the metadata name should be used");
	}
}
