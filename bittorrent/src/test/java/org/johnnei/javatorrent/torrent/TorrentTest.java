package org.johnnei.javatorrent.torrent;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageBitfield;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageHave;
import org.johnnei.javatorrent.disk.DiskJobCheckHash;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
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
	public void testGetRelevantPeers() {
		IDownloadPhase phaseMock = createMock(IDownloadPhase.class);
		expect(phaseMock.getRelevantPeers(notNull())).andReturn(Collections.emptyList());

		replayAll();

		Torrent cut = new Torrent.Builder()
				.setName("Torrent")
				.setHash(DummyEntity.createRandomBytes(20))
				.setInitialPhase(phaseMock)
				.build();

		assertNotNull("A collection should be returned", cut.getRelevantPeers());

		verifyAll();
	}

	@Test
	public void testAddPeer() throws Exception {
		byte[] peerId = DummyEntity.createUniquePeerId();
		byte[] peerIdTwo = DummyEntity.createUniquePeerId(peerId);

		BitTorrentSocket socketMockOne = createMock(BitTorrentSocket.class);
		socketMockOne.setPassedHandshake();

		BitTorrentSocket socketMockTwo = createMock(BitTorrentSocket.class);
		socketMockTwo.setPassedHandshake();

		BitTorrentSocket socketMockThree = createMock(BitTorrentSocket.class);
		socketMockThree.close();

		Torrent cut = DummyEntity.createUniqueTorrent();

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

}