package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBitfield;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.disk.DiskJobCheckHash;
import org.johnnei.javatorrent.disk.DiskJobWriteBlock;
import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.aryEq;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.notNull;
import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link Torrent}
 */
public class TorrentTest extends EasyMockSupport {

	private static final String METADATA_FILE = "gimp-2.8.16-setup-1.exe.torrent";

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
				.setHash(base.getHashArray())
				.build();

		TestUtils.assertEqualityMethods(base, equal, notEqual);
	}

	@Test
	public void testAddDiskJob() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Piece pieceMock = createMock(Piece.class);

		torrentClientMock.addDiskJob(notNull());

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("Add Disk Job Test")
				.setTorrentClient(torrentClientMock)
				.build();

		cut.addDiskJob(new DiskJobCheckHash(pieceMock, job -> {}));

		verifyAll();
	}

	@Test
	public void testCheckForProgress() throws Exception {
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		Piece pieceMockOne = createMock(Piece.class);
		Piece pieceMockTwo = createMock(Piece.class);
		Piece pieceMockThree = createMock(Piece.class);

		expect(fileSetMock.getNeededPieces()).andReturn(Arrays.asList(pieceMockOne, pieceMockTwo, pieceMockThree).stream());

		expect(pieceMockOne.checkHash()).andReturn(true);
		expect(pieceMockOne.getIndex()).andReturn(0).atLeastOnce();
		expect(pieceMockTwo.checkHash()).andReturn(false);
		expect(pieceMockThree.checkHash()).andThrow(new IOException("Test Check For Progress IO Exception"));
		expect(pieceMockThree.getIndex()).andReturn(2).atLeastOnce();
		fileSetMock.setHavingPiece(eq(0));

		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		Peer peerMock = createMock(Peer.class);

		expect(peerMock.getBitTorrentSocket()).andStubReturn(socketMock);
		socketMock.setPassedHandshake();
		socketMock.enqueueMessage(isA(MessageHave.class));

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("Check for progress test")
				.build();
		cut.setFileSet(fileSetMock);

		cut.addPeer(peerMock);
		cut.checkProgress();

		verifyAll();
	}

	@Test
	public void testSeederLeecherCount() throws Exception {
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		expect(fileSetMock.getPieceCount()).andReturn(5).atLeastOnce();

		MetadataFileSet metadataMock = createMock(MetadataFileSet.class);
		expect(metadataMock.isDone()).andReturn(true).atLeastOnce();

		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		Peer peerMock = createMock(Peer.class);

		expect(peerMock.getBitTorrentSocket()).andStubReturn(socketMock);
		expect(peerMock.countHavePieces()).andReturn(5).atLeastOnce();
		socketMock.setPassedHandshake();

		BitTorrentSocket socketMockTwo = createMock(BitTorrentSocket.class);
		Peer peerMockTwo = createMock(Peer.class);

		expect(peerMockTwo.getBitTorrentSocket()).andStubReturn(socketMockTwo);
		expect(peerMockTwo.countHavePieces()).andReturn(3).atLeastOnce();
		socketMockTwo.setPassedHandshake();

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("Upload/Download rates test")
				.build();

		cut.addPeer(peerMock);
		cut.addPeer(peerMockTwo);

		assertEquals("All peers should be leechers at this point.", 2, cut.getLeecherCount());
		assertEquals("We don't know if any peers are seeders yet.", 0, cut.getSeedCount());

		cut.setMetadata(metadataMock);
		cut.setFileSet(fileSetMock);

		assertEquals("All peers should be leechers at this point.", 1, cut.getLeecherCount());
		assertEquals("We don't know if any peers are seeders yet.", 1, cut.getSeedCount());

		verifyAll();
	}

	@Test
	public void testUploadDownloadRates() throws Exception {
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		Peer peerMock = createMock(Peer.class);

		expect(peerMock.getBitTorrentSocket()).andStubReturn(socketMock);
		expect(socketMock.getDownloadRate()).andReturn(5);
		expect(socketMock.getUploadRate()).andReturn(3);
		socketMock.pollRates();
		socketMock.setPassedHandshake();

		BitTorrentSocket socketMockTwo = createMock(BitTorrentSocket.class);
		Peer peerMockTwo = createMock(Peer.class);

		expect(peerMockTwo.getBitTorrentSocket()).andStubReturn(socketMockTwo);
		expect(socketMockTwo.getDownloadRate()).andReturn(10);
		expect(socketMockTwo.getUploadRate()).andReturn(13);
		socketMockTwo.pollRates();
		socketMockTwo.setPassedHandshake();

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("Upload/Download rates test")
				.build();

		cut.addPeer(peerMock);
		cut.addPeer(peerMockTwo);
		cut.pollRates();

		assertEquals("Download speed aren't added up correctly", 15, cut.getDownloadRate());
		assertEquals("Upload speed aren't added up correctly", 16, cut.getUploadRate());

		verifyAll();
	}

	@Test
	public void testOnReceivedBlockChechHashMismatch() throws Exception {
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		MetadataFileSet metadataMock = createMock(MetadataFileSet.class);
		TorrentClient torrentClient = createMock(TorrentClient.class);
		Piece pieceMock = createMock(Piece.class);

		Capture<IDiskJob> writeJobCapture = EasyMock.newCapture();
		Capture<IDiskJob> checkHashCapture = EasyMock.newCapture();

		expect(metadataMock.isDone()).andReturn(true);
		expect(fileSetMock.getBlockSize()).andReturn(15);
		expect(fileSetMock.getPiece(eq(0))).andReturn(pieceMock).atLeastOnce();
		expect(pieceMock.getBlockSize(eq(1))).andReturn(15);
		pieceMock.onHashMismatch();
		torrentClient.addDiskJob(and(isA(DiskJobWriteBlock.class), capture(writeJobCapture)));
		torrentClient.addDiskJob(and(isA(DiskJobCheckHash.class), capture(checkHashCapture)));

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		expect(pieceMock.checkHash()).andReturn(false);
		expect(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).andReturn(2);
		expect(pieceMock.getBlockCount()).andReturn(2);

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);
		cut.setMetadata(metadataMock);

		cut.onReceivedBlock(0, 15, new byte[15]);

		writeJobCapture.getValue().process();
		checkHashCapture.getValue().process();

		verifyAll();
	}

	@Test
	public void testOnReceivedBlockChechHash() throws Exception {
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		MetadataFileSet metadataMock = createMock(MetadataFileSet.class);
		TorrentClient torrentClient = createMock(TorrentClient.class);
		Piece pieceMock = createMock(Piece.class);

		Capture<IDiskJob> writeJobCapture = EasyMock.newCapture();
		Capture<IDiskJob> checkHashCapture = EasyMock.newCapture();

		expect(metadataMock.isDone()).andReturn(true).atLeastOnce();
		expect(fileSetMock.getBlockSize()).andReturn(15);
		expect(fileSetMock.getPiece(eq(0))).andReturn(pieceMock).atLeastOnce();
		expect(pieceMock.getBlockSize(eq(1))).andReturn(15);
		expect(pieceMock.getIndex()).andReturn(1).atLeastOnce();
		expect(pieceMock.getSize()).andReturn(15);
		fileSetMock.setHavingPiece(eq(1));
		torrentClient.addDiskJob(and(isA(DiskJobWriteBlock.class), capture(writeJobCapture)));
		torrentClient.addDiskJob(and(isA(DiskJobCheckHash.class), capture(checkHashCapture)));

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		expect(pieceMock.checkHash()).andReturn(true);
		expect(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).andReturn(2);
		expect(pieceMock.getBlockCount()).andReturn(2);

		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		Peer peerMock = createMock(Peer.class);

		expect(peerMock.getBitTorrentSocket()).andStubReturn(socketMock);
		socketMock.setPassedHandshake();
		expect(fileSetMock.countCompletedPieces()).andReturn(0);
		socketMock.enqueueMessage(isA(MessageHave.class));

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);
		cut.setMetadata(metadataMock);

		cut.addPeer(peerMock);
		cut.onReceivedBlock(0, 15, new byte[15]);

		assertEquals("Incorrect downloaded bytes, nothing is completed yet.", 0, cut.getDownloadedBytes());

		writeJobCapture.getValue().process();
		checkHashCapture.getValue().process();

		assertEquals("Incorrect downloaded bytes, piece size should have been added.", 15, cut.getDownloadedBytes());

		verifyAll();
	}

	@Test
	public void testOnReceivedBlockIncorrectSize() throws Exception {
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		TorrentClient torrentClient = createMock(TorrentClient.class);
		Piece pieceMock = createMock(Piece.class);

		expect(fileSetMock.getBlockSize()).andReturn(15);
		expect(fileSetMock.getPiece(eq(0))).andReturn(pieceMock).atLeastOnce();
		expect(pieceMock.getBlockSize(eq(1))).andReturn(10);

		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Needed));

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.onReceivedBlock(0, 15, new byte[15]);

		verifyAll();
	}

	@Test
	public void testOnReceivedBlockPieceNotDone() throws Exception {
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		TorrentClient torrentClient = createMock(TorrentClient.class);
		Piece pieceMock = createMock(Piece.class);

		Capture<IDiskJob> writeJobCapture = EasyMock.newCapture();

		expect(fileSetMock.getBlockSize()).andReturn(15);
		expect(fileSetMock.getPiece(eq(0))).andReturn(pieceMock).atLeastOnce();
		expect(pieceMock.getBlockSize(eq(1))).andReturn(15);
		torrentClient.addDiskJob(and(isA(DiskJobWriteBlock.class), capture(writeJobCapture)));

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		expect(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).andReturn(1);
		expect(pieceMock.getBlockCount()).andReturn(2);

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.onReceivedBlock(0, 15, new byte[15]);

		writeJobCapture.getValue().process();

		verifyAll();
	}

	@Test
	public void testOnReceivedBlockChechHashDownloadingMetadata() throws Exception {
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		TorrentClient torrentClient = createMock(TorrentClient.class);
		Piece pieceMock = createMock(Piece.class);

		Capture<IDiskJob> writeJobCapture = EasyMock.newCapture();
		Capture<IDiskJob> checkHashCapture = EasyMock.newCapture();

		expect(fileSetMock.getBlockSize()).andReturn(15);
		expect(fileSetMock.getPiece(eq(0))).andReturn(pieceMock).atLeastOnce();
		expect(pieceMock.getBlockSize(eq(1))).andReturn(15);
		torrentClient.addDiskJob(and(isA(DiskJobWriteBlock.class), capture(writeJobCapture)));
		torrentClient.addDiskJob(and(isA(DiskJobCheckHash.class), capture(checkHashCapture)));

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		expect(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).andReturn(2);
		expect(pieceMock.getBlockCount()).andReturn(2);
		expect(pieceMock.checkHash()).andReturn(true);

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.onReceivedBlock(0, 15, new byte[15]);

		writeJobCapture.getValue().process();
		checkHashCapture.getValue().process();

		verifyAll();
	}

	@Test
	public void testOnReceivedBlock() throws Exception {
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);
		TorrentClient torrentClient = createMock(TorrentClient.class);
		Piece pieceMock = createMock(Piece.class);

		Capture<IDiskJob> writeJobCapture = EasyMock.newCapture();
		Capture<IDiskJob> checkHashCapture = EasyMock.newCapture();

		expect(fileSetMock.getBlockSize()).andReturn(15);
		expect(fileSetMock.getPiece(eq(0))).andReturn(pieceMock).atLeastOnce();
		expect(pieceMock.getBlockSize(eq(1))).andReturn(15);
		torrentClient.addDiskJob(and(isA(DiskJobWriteBlock.class), capture(writeJobCapture)));
		torrentClient.addDiskJob(and(isA(DiskJobCheckHash.class), capture(checkHashCapture)));

		pieceMock.storeBlock(eq(1), aryEq(new byte[15]));
		pieceMock.setBlockStatus(eq(1), eq(BlockStatus.Stored));
		expect(pieceMock.countBlocksWithStatus(eq(BlockStatus.Stored))).andReturn(2);
		expect(pieceMock.getBlockCount()).andReturn(2);

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("On Received Block Test")
				.setTorrentClient(torrentClient)
				.build();
		cut.setFileSet(fileSetMock);

		cut.onReceivedBlock(0, 15, new byte[15]);

		writeJobCapture.getValue().process();

		verifyAll();
	}

	@Test
	public void testGetHash() {
		Torrent cut = DummyEntity.createUniqueTorrent();

		assertTrue("Incorrect hash output", cut.getHash().matches("^[a-fA-F0-9]{40}$"));
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

		BitTorrentSocket socketMockOne = createMock(BitTorrentSocket.class);
		socketMockOne.setPassedHandshake();

		BitTorrentSocket socketMockTwo = createMock(BitTorrentSocket.class);
		socketMockTwo.setPassedHandshake();

		BitTorrentSocket socketMockThree = createMock(BitTorrentSocket.class);
		socketMockThree.close();

		Torrent cut = DummyEntity.createUniqueTorrent();

		Peer peerMock = createMockBuilder(Peer.class)
				.addMockedMethod("discardAllBlockRequests")
				.createMock();
		Whitebox.setInternalState(peerMock, "id", peerIdTwo);
		peerMock.discardAllBlockRequests();

		replayAll();

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

		cut.removePeer(peerMock);

		assertEquals("Peer two should have been removed from the list.", 1, cut.getPeers().size());
		assertFalse("Peer two should have been removed to the list.", cut.getPeers().contains(peerTwo));

		cut.removePeer(peerTwo);

		assertEquals("Removal of peer two twice should not affect the list on the second attempt.", 1, cut.getPeers().size());

		verifyAll();
	}

	private MetadataFileSet createCompletedMetadataFileSet(Torrent torrent) throws Exception {
		File file = new File(TorrentTest.class.getResource(METADATA_FILE).toURI());
		MetadataFileSet metadataFileSet = new MetadataFileSet(torrent, file);

		metadataFileSet.getNeededPieces().forEach(p -> metadataFileSet.setHavingPiece(p.getIndex()));
		return metadataFileSet;
	}

	@Test
	public void testSendHaveMessagesNoMessages() throws Exception {
		byte[] peerId = DummyEntity.createUniquePeerId();

		BitTorrentSocket socketMockOne = createMock(BitTorrentSocket.class);
		socketMockOne.setPassedHandshake();

		TorrentFileSet fileSetMock = createMock(TorrentFileSet.class);
		expect(fileSetMock.countCompletedPieces()).andReturn(0).atLeastOnce();
		expect(fileSetMock.getBitfieldBytes()).andReturn(new byte[0]);

		replayAll();

		Torrent cut = DummyEntity.createUniqueTorrent();
		cut.setMetadata(createCompletedMetadataFileSet(cut));
		cut.setFileSet(fileSetMock);

		Peer peer = new Peer.Builder()
				.setSocket(socketMockOne)
				.setTorrent(cut)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		cut.addPeer(peer);

		verifyAll();
	}

	@Test
	public void testSendHaveMessagesSendBitfield() throws Exception {
		byte[] peerId = DummyEntity.createUniquePeerId();

		BitTorrentSocket socketMockOne = createMock(BitTorrentSocket.class);
		socketMockOne.setPassedHandshake();
		socketMockOne.enqueueMessage(isA(MessageBitfield.class));

		TorrentFileSet fileSetMock = createMock(TorrentFileSet.class);
		expect(fileSetMock.countCompletedPieces()).andReturn(7).atLeastOnce();
		expect(fileSetMock.getBitfieldBytes()).andReturn(new byte[2]).atLeastOnce();

		replayAll();

		Torrent cut = DummyEntity.createUniqueTorrent();
		cut.setMetadata(createCompletedMetadataFileSet(cut));
		cut.setFileSet(fileSetMock);

		Peer peer = new Peer.Builder()
				.setSocket(socketMockOne)
				.setTorrent(cut)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		cut.addPeer(peer);

		verifyAll();
	}

	@Test
	public void testSendHaveMessagesSendHaveMessages() throws Exception {
		byte[] peerId = DummyEntity.createUniquePeerId();

		Capture<MessageHave> haveCapture = EasyMock.newCapture(CaptureType.ALL);

		BitTorrentSocket socketMockOne = createMock(BitTorrentSocket.class);
		socketMockOne.setPassedHandshake();
		socketMockOne.enqueueMessage(capture(haveCapture));
		socketMockOne.enqueueMessage(capture(haveCapture));

		TorrentFileSet fileSetMock = createMock(TorrentFileSet.class);
		expect(fileSetMock.countCompletedPieces()).andReturn(2).atLeastOnce();
		expect(fileSetMock.getBitfieldBytes()).andReturn(new byte[12]).atLeastOnce();
		expect(fileSetMock.getPieceCount()).andReturn(96).atLeastOnce();
		expect(fileSetMock.hasPiece(0)).andReturn(true);
		expect(fileSetMock.hasPiece(5)).andReturn(true);
		expect(fileSetMock.hasPiece(anyInt())).andReturn(false).times(94);

		replayAll();

		Torrent cut = DummyEntity.createUniqueTorrent();
		cut.setMetadata(createCompletedMetadataFileSet(cut));
		cut.setFileSet(fileSetMock);

		Peer peer = new Peer.Builder()
				.setSocket(socketMockOne)
				.setTorrent(cut)
				.setExtensionBytes(DummyEntity.createRandomBytes(8))
				.setId(peerId)
				.build();

		cut.addPeer(peer);

		verifyAll();

		List<Integer> expectedPieces = new ArrayList<>();
		expectedPieces.add(0);
		expectedPieces.add(5);
		for (MessageHave message : haveCapture.getValues()) {
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

		builder.setHash(DummyEntity.createRandomBytes(20));

		assertTrue("Torrent should not be downloadable yet", builder.canDownload());
	}

	@Test
	public void testGetDisplayName() {
		Torrent cut = new Torrent.Builder()
				.setName("Test")
				.setHash(DummyEntity.createUniqueTorrentHash())
				.build();

		assertEquals("Incorrect display name", "Test", cut.getDisplayName());
	}

	@Test
	public void testGetUploadedBytes() {
		Torrent cut = new Torrent.Builder()
				.setName("Test")
				.setHash(DummyEntity.createUniqueTorrentHash())
				.build();

		assertEquals("Incorrect amount of uploaded bytes, nothing is uploaded", 0, cut.getUploadedBytes());
		cut.addUploadedBytes(15);
		assertEquals("Incorrect amount of uploaded bytes, data has been uploaded", 15, cut.getUploadedBytes());
	}

	@Test
	public void testGetMetadata() {
		MetadataFileSet metadataFileSetMock = createMock(MetadataFileSet.class);

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("Test")
				.setHash(DummyEntity.createUniqueTorrentHash())
				.build();

		assertNotPresent("Metadata has not been set yet", cut.getMetadata());

		cut.setMetadata(metadataFileSetMock);

		assertPresent("Metadata has been set", cut.getMetadata());
		assertTrue("Metadata should be the same reference as the one set.", metadataFileSetMock == cut.getMetadata().get());

		verifyAll();
	}

	@Test
	public void testIsDownloadingMetadata() {
		MetadataFileSet metadataFileSetMock = createMock(MetadataFileSet.class);

		expect(metadataFileSetMock.isDone()).andReturn(false);
		expect(metadataFileSetMock.isDone()).andReturn(true);

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("Test")
				.setHash(DummyEntity.createUniqueTorrentHash())
				.build();

		assertTrue("Metadata has not been set yet", cut.isDownloadingMetadata());

		cut.setMetadata(metadataFileSetMock);

		assertTrue("Metadata should have returned that it is not done yet", cut.isDownloadingMetadata());
		assertFalse("Metadata should have returned that it is done", cut.isDownloadingMetadata());

		verifyAll();
	}
}