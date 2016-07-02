package org.johnnei.javatorrent.internal.torrent;

import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.tracker.TrackerManager;
import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.choking.IChokingStrategy;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.same;

/**
 * Tests {@link TorrentProcessor}
 */
public class TorrentProcessorTest extends EasyMockSupport {


	private TorrentManager managerMock;
	private TorrentClient torrentClient;
	private IDownloadPhase phaseMock;
	private PhaseRegulator phaseRegulatorMock;
	private ScheduledFuture futureMock;
	private ScheduledExecutorService executorServiceMock;

	@Before
	public void setUp() {
		managerMock = createMock(TorrentManager.class);
		torrentClient = createMock(TorrentClient.class);

		phaseMock = createMock(IDownloadPhase.class);
		phaseMock.onPhaseEnter();
		phaseRegulatorMock = createMock(PhaseRegulator.class);
		executorServiceMock = createMock(ScheduledExecutorService.class);
		futureMock = createMock(ScheduledFuture.class);

		expect(torrentClient.getPhaseRegulator()).andStubReturn(phaseRegulatorMock);
		expect(torrentClient.getExecutorService()).andStubReturn(executorServiceMock);
		expect(phaseRegulatorMock.createInitialPhase(notNull(), notNull())).andReturn(phaseMock);
		expect(executorServiceMock.scheduleAtFixedRate(notNull(), anyLong(), anyLong(), notNull())).andStubReturn(futureMock);
	}

	@Test
	public void testUpdateTorrentStateEnd() {
		Torrent torrentMock = createMock(Torrent.class);
		TrackerManager trackerManagerMock = createMock(TrackerManager.class);

		expect(phaseMock.isDone()).andReturn(true);
		phaseMock.onPhaseExit();
		expect(phaseRegulatorMock.createNextPhase(same(phaseMock), same(torrentClient), same(torrentMock))).andReturn(Optional.empty());
		expect(futureMock.cancel(eq(false))).andReturn(true).times(4);
		managerMock.removeTorrent(same(torrentMock));

		replayAll();
		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.updateTorrentState();

		verifyAll();
	}

	@Test
	public void testUpdateTorrentStateChangePhase() {
		Torrent torrentMock = createMock(Torrent.class);
		TrackerManager trackerManagerMock = createMock(TrackerManager.class);
		IDownloadPhase phaseTwoMock = createMock(IDownloadPhase.class);

		expect(phaseMock.isDone()).andReturn(true);
		phaseMock.onPhaseExit();
		expect(phaseRegulatorMock.createNextPhase(same(phaseMock), same(torrentClient), same(torrentMock))).andReturn(Optional.of(phaseTwoMock));
		phaseTwoMock.onPhaseEnter();
		phaseTwoMock.process();

		replayAll();
		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.updateTorrentState();

		verifyAll();
	}

	@Test
	public void testUpdateTorrentState() {
		Torrent torrentMock = createMock(Torrent.class);
		TrackerManager trackerManagerMock = createMock(TrackerManager.class);

		expect(phaseMock.isDone()).andReturn(false);
		phaseMock.process();

		replayAll();
		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.updateTorrentState();

		verifyAll();
	}

	@Test
	public void testUpdateChokingStates() {
		Torrent torrentMock = createMock(Torrent.class);
		Peer peerMock = createMock(Peer.class);
		IChokingStrategy chokingStrategyMock = createMock(IChokingStrategy.class);
		TrackerManager trackerManagerMock = createMock(TrackerManager.class);

		expect(torrentMock.getPeers()).andReturn(Collections.singletonList(peerMock));
		expect(phaseMock.getChokingStrategy()).andReturn(chokingStrategyMock);
		chokingStrategyMock.updateChoking(same(peerMock));

		replayAll();
		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.updateChokingStates();

		verifyAll();
	}

	@Test
	public void testRemoveDisconnectedPeers() {
		Torrent torrentMock = createMock(Torrent.class);
		Peer peerMock = createMock(Peer.class);
		BitTorrentSocket socketMock = createMock(BitTorrentSocket.class);
		TrackerManager trackerManagerMock = createMock(TrackerManager.class);

		expect(torrentMock.getPeers()).andReturn(Collections.singletonList(peerMock));
		expect(peerMock.getBitTorrentSocket()).andReturn(socketMock);
		expect(socketMock.closed()).andReturn(true);
		torrentMock.removePeer(same(peerMock));

		replayAll();
		TorrentProcessor processor = new TorrentProcessor(managerMock, trackerManagerMock, torrentClient, torrentMock);
		processor.removeDisconnectedPeers();

		verifyAll();
	}

}