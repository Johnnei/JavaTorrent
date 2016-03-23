package org.johnnei.javatorrent.torrent;

import java.util.Collections;
import java.util.Optional;

import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
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

}