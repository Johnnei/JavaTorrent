package org.johnnei.javatorrent.phases;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.torrent.AbstractFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.pieceselector.FullPieceSelect;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(EasyMockRunner.class)
public class PhaseDataTest extends EasyMockSupport {

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

		Torrent torrent = DummyEntity.createUniqueTorrent();
		torrent.setFileSet(StubEntity.stubAFiles(5));

		List<Peer> peerList = Arrays.asList(peerOne, peerTwo, peerThree, peerFour, peerFive);


		PhaseData cut = new PhaseData(torrentClientMock, torrent);
		Collection<Peer> relevantPeers = cut.getRelevantPeers(peerList);

		assertEquals("Incorrect amount of peers", 3, relevantPeers.size());
		assertTrue("Relevant peer is missing", relevantPeers.contains(peerTwo));
		assertTrue("Relevant peer is missing", relevantPeers.contains(peerThree));
		assertTrue("Relevant peer is missing", relevantPeers.contains(peerFive));

		verifyAll();
	}

	private ITracker createTrackerExpectingSetCompleted(Torrent torrent) {
		ITracker trackerMock = createMock(ITracker.class);
		TorrentInfo torrentInfoMock = createMock(TorrentInfo.class);

		expect(trackerMock.getInfo(same(torrent))).andReturn(Optional.of(torrentInfoMock));
		torrentInfoMock.setEvent(eq(TrackerEvent.EVENT_COMPLETED));
		return trackerMock;
	}

	@Test
	public void testOnPhaseExit() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);

		ITracker trackerOne = createTrackerExpectingSetCompleted(torrentMock);
		ITracker trackerTwo = createTrackerExpectingSetCompleted(torrentMock);

		expect(torrentClientMock.getTrackersFor(same(torrentMock))).andReturn(Arrays.asList(trackerOne, trackerTwo));

		replayAll();

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.onPhaseExit();

		verifyAll();
	}

	@Test
	public void testOnPhaseEnter() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);

		torrentMock.checkProgress();
		torrentMock.setPieceSelector(isA(FullPieceSelect.class));

		replayAll();

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verifyAll();
	}

	@Test
	public void testIsDone() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		expect(torrentMock.getFileSet()).andReturn(fileSetMock);
		expect(fileSetMock.isDone()).andReturn(true);

		replayAll();

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);

		boolean result = cut.isDone();

		verifyAll();
		assertTrue("File set should have returned done, so the phase should have been done", result);
	}

	@Test
	public void testProcessTestConcurrentBlockGiveaway() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);
		IPieceSelector pieceSelectorMock = createMock(IPieceSelector.class);
		BitTorrentSocket bitTorrentSocketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		Piece pieceMock = createMock(Piece.class);

		expect(pieceMock.hasBlockWithStatus(eq(BlockStatus.Needed))).andReturn(true);
		expect(pieceMock.getRequestBlock()).andReturn(Optional.empty());
		expect(pieceMock.getIndex()).andReturn(0).atLeastOnce();

		Peer peer = DummyEntity.createPeer(bitTorrentSocketMock);
		peer.setRequestLimit(1);
		peer.setHavingPiece(0);

		expect(torrentMock.getPeers()).andReturn(Collections.singletonList(peer));
		expect(torrentMock.getPieceSelector()).andReturn(pieceSelectorMock);
		expect(torrentMock.getFileSet()).andReturn(fileSetMock);
		expect(pieceSelectorMock.getPieceForPeer(same(peer))).andReturn(Optional.of(pieceMock));
		expect(fileSetMock.getNeededPieces()).andReturn(Collections.singletonList(pieceMock).stream());

		replayAll();

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.process();

		verifyAll();
	}

	@Test
	public void testProcessHitRequestLimit() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);
		IPieceSelector pieceSelectorMock = createMock(IPieceSelector.class);
		BitTorrentSocket bitTorrentSocketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		Piece piece = new Piece(null, null, 0, 8, 4);

		Peer peer = DummyEntity.createPeer(bitTorrentSocketMock);
		peer.setRequestLimit(1);
		peer.setHavingPiece(0);

		expect(torrentMock.getPeers()).andReturn(Collections.singletonList(peer));
		expect(torrentMock.getPieceSelector()).andReturn(pieceSelectorMock);
		expect(pieceSelectorMock.getPieceForPeer(same(peer))).andReturn(Optional.of(piece));
		bitTorrentSocketMock.enqueueMessage(isA(MessageRequest.class));
		expect(torrentMock.getFileSet()).andReturn(fileSetMock).atLeastOnce();
		expect(fileSetMock.getBlockSize()).andReturn(4).atLeastOnce();
		expect(fileSetMock.getNeededPieces()).andReturn(Collections.singletonList(piece).stream());


		replayAll();

		Whitebox.setInternalState(peer, Torrent.class, torrentMock);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.process();

		verifyAll();
	}

	@Test
	public void testProcess() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);
		IPieceSelector pieceSelectorMock = createMock(IPieceSelector.class);
		BitTorrentSocket bitTorrentSocketMock = createMock(BitTorrentSocket.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		Piece piece = new Piece(null, null, 0, 8, 4);

		Peer peer = DummyEntity.createPeer(bitTorrentSocketMock);
		peer.setRequestLimit(2);
		peer.setHavingPiece(0);

		expect(torrentMock.getPeers()).andReturn(Collections.singletonList(peer));
		expect(torrentMock.getPieceSelector()).andReturn(pieceSelectorMock);
		expect(pieceSelectorMock.getPieceForPeer(same(peer))).andReturn(Optional.of(piece));
		bitTorrentSocketMock.enqueueMessage(isA(MessageRequest.class));
		expectLastCall().times(2);
		expect(torrentMock.getFileSet()).andReturn(fileSetMock).atLeastOnce();
		expect(fileSetMock.getBlockSize()).andReturn(4).atLeastOnce();
		expect(fileSetMock.getNeededPieces()).andReturn(Collections.singletonList(piece).stream());

		replayAll();

		Whitebox.setInternalState(peer, Torrent.class, torrentMock);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.process();

		verifyAll();
	}

	@Test
	public void testProcessNoPieceReturned() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		Torrent torrentMock = createMock(Torrent.class);
		IPieceSelector pieceSelectorMock = createMock(IPieceSelector.class);
		AbstractFileSet fileSetMock = createMock(AbstractFileSet.class);

		Peer peerMock = createMock(Peer.class);
		Piece piece = new Piece(null, null, 0, 8, 4);

		expect(torrentMock.getPeers()).andReturn(Collections.singletonList(peerMock));
		expect(torrentMock.getPieceSelector()).andReturn(pieceSelectorMock);
		expect(pieceSelectorMock.getPieceForPeer(same(peerMock))).andReturn(Optional.empty());
		expect(torrentMock.getFileSet()).andReturn(fileSetMock);
		expect(peerMock.hasPiece(eq(0))).andReturn(true);
		expect(fileSetMock.getNeededPieces()).andReturn(Collections.singletonList(piece).stream());

		replayAll();

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.process();

		verifyAll();
	}
}
