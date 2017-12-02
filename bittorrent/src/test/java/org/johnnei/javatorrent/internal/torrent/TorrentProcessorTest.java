package org.johnnei.javatorrent.internal.torrent;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.tracker.TrackerManager;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests {@link TorrentProcessor}
 */
public class TorrentProcessorTest {


	private TorrentManager managerMock;
	private TorrentClient torrentClient;
	private IDownloadPhase phaseMock;
	private PhaseRegulator phaseRegulatorMock;
	private ScheduledFuture futureMock;
	private ScheduledExecutorService executorServiceMock;

	@BeforeEach
	public void setUp() {
		managerMock = mock(TorrentManager.class);
		torrentClient = mock(TorrentClient.class);

		phaseMock = mock(IDownloadPhase.class);
		phaseMock.onPhaseEnter();
		phaseRegulatorMock = mock(PhaseRegulator.class);
		executorServiceMock = mock(ScheduledExecutorService.class);
		futureMock = mock(ScheduledFuture.class);

		when(torrentClient.getPhaseRegulator()).thenReturn(phaseRegulatorMock);
		when(torrentClient.getExecutorService()).thenReturn(executorServiceMock);
		when(phaseRegulatorMock.createInitialPhase(notNull(), notNull())).thenReturn(phaseMock);
		when(executorServiceMock.scheduleAtFixedRate(notNull(), anyLong(), anyLong(), notNull())).thenReturn(futureMock);
	}

	@Test
	public void testUpdateTorrentStateEnd() {
		Torrent torrentMock = mock(Torrent.class);
		TrackerManager trackerManagerMock = mock(TrackerManager.class);

		when(phaseMock.isDone()).thenReturn(true);
		when(phaseRegulatorMock.createNextPhase(same(phaseMock), same(torrentClient), same(torrentMock))).thenReturn(Optional.empty());
		when(futureMock.cancel(eq(false))).thenReturn(true);

		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.updateTorrentState();

		verify(phaseMock).onPhaseExit();
		verify(managerMock).removeTorrent(same(torrentMock));
	}

	@Test
	public void testUpdateTorrentStateChangePhase() {
		Torrent torrentMock = mock(Torrent.class);
		TrackerManager trackerManagerMock = mock(TrackerManager.class);
		IDownloadPhase phaseTwoMock = mock(IDownloadPhase.class);

		when(phaseMock.isDone()).thenReturn(true);
		when(phaseRegulatorMock.createNextPhase(same(phaseMock), same(torrentClient), same(torrentMock))).thenReturn(Optional.of(phaseTwoMock));

		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.updateTorrentState();

		verify(phaseMock).onPhaseExit();
		verify(phaseTwoMock).onPhaseEnter();
		verify(phaseTwoMock).process();
	}

	@Test
	public void testUpdateTorrentState() {
		Torrent torrentMock = mock(Torrent.class);
		TrackerManager trackerManagerMock = mock(TrackerManager.class);

		when(phaseMock.isDone()).thenReturn(false);

		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.updateTorrentState();

		verify(phaseMock).process();
	}

	@Test
	public void testUpdateChokingStates() {
		Torrent torrentMock = mock(Torrent.class);
		Peer peerMock = mock(Peer.class);
		IChokingStrategy chokingStrategyMock = mock(IChokingStrategy.class);
		TrackerManager trackerManagerMock = mock(TrackerManager.class);

		when(torrentMock.getPeers()).thenReturn(Collections.singletonList(peerMock));
		when(phaseMock.getChokingStrategy()).thenReturn(chokingStrategyMock);

		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.updateChokingStates();

		verify(chokingStrategyMock).updateChoking(same(peerMock));
	}

	@Test
	public void testRemoveDisconnectedPeers() {
		Torrent torrentMock = mock(Torrent.class);
		Peer peerMock = mock(Peer.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		TrackerManager trackerManagerMock = mock(TrackerManager.class);

		when(torrentMock.getPeers()).thenReturn(Collections.singletonList(peerMock));
		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		when(socketMock.closed()).thenReturn(true);

		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.removeDisconnectedPeers();

		verify(torrentMock).removePeer(same(peerMock));
	}

}
