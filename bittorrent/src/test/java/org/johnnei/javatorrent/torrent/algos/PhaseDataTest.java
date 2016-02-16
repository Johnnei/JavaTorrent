package org.johnnei.javatorrent.torrent.algos;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.phases.PhaseData;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class PhaseDataTest extends EasyMockSupport {

	private PhaseData cut;

	@Test
	public void testGetRelevantPeers() {
		TorrentClient torrentClientMock = StubEntity.stubTorrentClient(this);

		Peer peerOne = createMock(Peer.class);
		Peer peerTwo = createMock(Peer.class);
		Peer peerThree = createMock(Peer.class);
		Peer peerFour = createMock(Peer.class);
		Peer peerFive = createMock(Peer.class);

		expect(peerOne.hasPiece(anyInt())).andReturn(false).atLeastOnce();
		expect(peerTwo.hasPiece(anyInt())).andReturn(true).atLeastOnce();
		expect(peerThree.hasPiece(anyInt())).andReturn(true).atLeastOnce();
		expect(peerFour.hasPiece(anyInt())).andReturn(false).atLeastOnce();
		expect(peerFive.hasPiece(anyInt())).andReturn(true).atLeastOnce();

		replayAll();

		Torrent torrent = DummyEntity.createTorrent();
		torrent.setFiles(StubEntity.stubAFiles(5));

		List<Peer> peerList = Arrays.asList(peerOne, peerTwo, peerThree, peerFour, peerFive);


		cut = new PhaseData(torrentClientMock, torrent);
		Collection<Peer> relevantPeers = cut.getRelevantPeers(peerList);

		assertEquals("Incorrect amount of peers", 3, relevantPeers.size());
		assertTrue("Relevant peer is missing", relevantPeers.contains(peerTwo));
		assertTrue("Relevant peer is missing", relevantPeers.contains(peerThree));
		assertTrue("Relevant peer is missing", relevantPeers.contains(peerFive));

		verifyAll();
	}

}
