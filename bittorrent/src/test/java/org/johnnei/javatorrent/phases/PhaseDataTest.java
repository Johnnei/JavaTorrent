package org.johnnei.javatorrent.phases;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.internal.torrent.TorrentFileSetRequestFactory;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.algos.pieceselector.FullPieceSelect;
import org.johnnei.javatorrent.torrent.algos.pieceselector.IPieceSelector;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PhaseDataTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testGetRelevantPeers() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);

		Peer peerOne = mock(Peer.class);
		Peer peerTwo = mock(Peer.class);
		Peer peerThree = mock(Peer.class);
		Peer peerFour = mock(Peer.class);
		Peer peerFive = mock(Peer.class);

		when(peerOne.hasPiece(anyInt())).thenReturn(false);
		when(peerTwo.hasPiece(anyInt())).thenReturn(true);
		when(peerThree.hasPiece(anyInt())).thenReturn(true);
		when(peerFour.hasPiece(anyInt())).thenReturn(false);
		when(peerFive.hasPiece(anyInt())).thenReturn(true);

		TorrentFileSet torrentFileSetMock = mock(TorrentFileSet.class);

		Torrent torrent = DummyEntity.createUniqueTorrent();
		torrent.setFileSet(torrentFileSetMock);

		List<Peer> peerList = Arrays.asList(peerOne, peerTwo, peerThree, peerFour, peerFive);

		Piece pieceMock = mock(Piece.class);
		when(pieceMock.getIndex()).thenReturn(1);

		Piece pieceMockTwo = mock(Piece.class);
		when(pieceMockTwo.getIndex()).thenReturn(1);

		when(torrentFileSetMock.getNeededPieces()).thenReturn(Stream.of(pieceMock, pieceMockTwo));

		PhaseData cut = new PhaseData(torrentClientMock, torrent);
		Collection<Peer> relevantPeers = cut.getRelevantPeers(peerList);

		assertEquals("Incorrect amount of peers", 3, relevantPeers.size());
		assertTrue("Relevant peer is missing", relevantPeers.contains(peerTwo));
		assertTrue("Relevant peer is missing", relevantPeers.contains(peerThree));
		assertTrue("Relevant peer is missing", relevantPeers.contains(peerFive));
	}

	private ITracker createTrackerExpectingSetCompleted(Torrent torrent) {
		ITracker trackerMock = mock(ITracker.class);
		TorrentInfo torrentInfoMock = mock(TorrentInfo.class);

		when(trackerMock.getInfo(same(torrent))).thenReturn(Optional.of(torrentInfoMock));
		torrentInfoMock.setEvent(eq(TrackerEvent.EVENT_COMPLETED));
		return trackerMock;
	}

	@Test
	public void testOnPhaseExit() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);

		ITracker trackerOne = createTrackerExpectingSetCompleted(torrentMock);
		ITracker trackerTwo = createTrackerExpectingSetCompleted(torrentMock);

		when(torrentClientMock.getTrackersFor(same(torrentMock))).thenReturn(Arrays.asList(trackerOne, trackerTwo));

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.onPhaseExit();
	}

	@Test
	public void testOnPhaseEnter() throws IOException {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet torrentFileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(torrentFileSetMock);
		when(torrentFileSetMock.getDownloadFolder()).thenReturn(temporaryFolder.newFolder());

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verify(torrentMock).checkProgress();
		verify(torrentMock).setPieceSelector(isA(FullPieceSelect.class));
	}

	@Test
	public void testOnPhaseEnterCreateFolder() throws IOException {
		File file = new File(temporaryFolder.getRoot(), "myFolder");

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet torrentFileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(torrentFileSetMock);
		when(torrentFileSetMock.getDownloadFolder()).thenReturn(file);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verify(torrentMock).checkProgress();
		verify(torrentMock).setPieceSelector(isA(FullPieceSelect.class));
		assertTrue("Download folder should have been created.", file.exists());
	}

	@Test
	public void testOnPhaseEnterCreateFolderFailure() throws IOException {
		thrown.expect(TorrentException.class);
		thrown.expectMessage("download folder");

		File fileMock = mock(File.class);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet torrentFileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(torrentFileSetMock);
		when(torrentFileSetMock.getDownloadFolder()).thenReturn(fileMock);

		when(fileMock.exists()).thenReturn(false);
		when(fileMock.mkdirs()).thenReturn(false);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.onPhaseEnter();

		verify(torrentMock).checkProgress();
		verify(torrentMock).setPieceSelector(isA(FullPieceSelect.class));
	}

	@Test
	public void testIsDone() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.isDone()).thenReturn(true);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);

		boolean result = cut.isDone();

		assertTrue("File set should have returned done, so the phase should have been done", result);
	}

	@Test
	public void testProcessTestConcurrentBlockGiveaway() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		IPieceSelector pieceSelectorMock = mock(IPieceSelector.class);
		BitTorrentSocket bitTorrentSocketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		Piece pieceMock = mock(Piece.class);

		when(pieceMock.hasBlockWithStatus(eq(BlockStatus.Needed))).thenReturn(true);
		when(pieceMock.getRequestBlock()).thenReturn(Optional.empty());
		when(pieceMock.getIndex()).thenReturn(0);

		Peer peer = DummyEntity.createPeer(bitTorrentSocketMock);
		peer.setRequestLimit(1);
		peer.setHavingPiece(0);

		when(torrentMock.getPeers()).thenReturn(Collections.singletonList(peer));
		when(torrentMock.getPieceSelector()).thenReturn(pieceSelectorMock);
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(pieceSelectorMock.getPieceForPeer(same(peer))).thenReturn(Optional.of(pieceMock));
		when(fileSetMock.getNeededPieces()).thenReturn(Collections.singletonList(pieceMock).stream());

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.process();
	}

	@Test
	public void testProcessHitRequestLimit() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		IPieceSelector pieceSelectorMock = mock(IPieceSelector.class);
		BitTorrentSocket bitTorrentSocketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentFileSetRequestFactory requestFactoryMock = mock(TorrentFileSetRequestFactory.class);

		Piece piece = new Piece(fileSetMock, null, 0, 8, 4);

		Peer peer = DummyEntity.createPeer(bitTorrentSocketMock);
		peer.setRequestLimit(1);
		peer.setHavingPiece(0);

		when(torrentMock.getPeers()).thenReturn(Collections.singletonList(peer));
		when(torrentMock.getPieceSelector()).thenReturn(pieceSelectorMock);
		when(pieceSelectorMock.getPieceForPeer(same(peer))).thenReturn(Optional.of(piece));
		when(fileSetMock.getRequestFactory()).thenReturn(requestFactoryMock);
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBlockSize()).thenReturn(4);
		when(fileSetMock.getNeededPieces()).thenReturn(Collections.singletonList(piece).stream());

		Whitebox.setInternalState(peer, Torrent.class, torrentMock);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.process();

		verify(bitTorrentSocketMock).enqueueMessage(any());
		verify(requestFactoryMock).createRequestFor(peer, piece, 0, 4);
	}

	@Test
	public void testProcess() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		IPieceSelector pieceSelectorMock = mock(IPieceSelector.class);
		BitTorrentSocket bitTorrentSocketMock = mock(BitTorrentSocket.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
		TorrentFileSetRequestFactory requestFactoryMock = mock(TorrentFileSetRequestFactory.class);

		Piece piece = new Piece(fileSetMock, null, 0, 8, 4);

		Peer peer = DummyEntity.createPeer(bitTorrentSocketMock);
		peer.setRequestLimit(2);
		peer.setHavingPiece(0);

		when(torrentMock.getPeers()).thenReturn(Collections.singletonList(peer));
		when(torrentMock.getPieceSelector()).thenReturn(pieceSelectorMock);
		when(pieceSelectorMock.getPieceForPeer(same(peer))).thenReturn(Optional.of(piece));
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(fileSetMock.getBlockSize()).thenReturn(4);
		when(fileSetMock.getRequestFactory()).thenReturn(requestFactoryMock);
		when(fileSetMock.getNeededPieces()).thenReturn(Collections.singletonList(piece).stream());

		Whitebox.setInternalState(peer, Torrent.class, torrentMock);

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.process();

		verify(bitTorrentSocketMock, times(2)).enqueueMessage(any());
		verify(requestFactoryMock).createRequestFor(peer, piece, 0, 4);
	}

	@Test
	public void testProcessNoPieceReturned() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		Torrent torrentMock = mock(Torrent.class);
		IPieceSelector pieceSelectorMock = mock(IPieceSelector.class);
		TorrentFileSet fileSetMock = mock(TorrentFileSet.class);

		Peer peerMock = mock(Peer.class);
		Piece piece = new Piece(null, null, 0, 8, 4);

		when(torrentMock.getPeers()).thenReturn(Collections.singletonList(peerMock));
		when(torrentMock.getPieceSelector()).thenReturn(pieceSelectorMock);
		when(pieceSelectorMock.getPieceForPeer(same(peerMock))).thenReturn(Optional.empty());
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(peerMock.hasPiece(eq(0))).thenReturn(true);
		when(fileSetMock.getNeededPieces()).thenReturn(Collections.singletonList(piece).stream());

		PhaseData cut = new PhaseData(torrentClientMock, torrentMock);
		cut.process();
	}
}
