package org.johnnei.javatorrent.internal.torrent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.internal.tracker.TrackerManager;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;

import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.same;
import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link TorrentManager}
 */
public class TorrentManagerTest extends EasyMockSupport {

	@Test
	public void testAddRemoveGetTorrent() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		ScheduledExecutorService executorServiceMock = createMock(ScheduledExecutorService.class);
		IDownloadPhase phaseMock = createMock(IDownloadPhase.class);
		PhaseRegulator regulatorMock = createMock(PhaseRegulator.class);
		ScheduledFuture futureMock = createMock(ScheduledFuture.class);
		TrackerManager trackerManager = createMock(TrackerManager.class);

		expect(torrentClientMock.getExecutorService()).andReturn(executorServiceMock).atLeastOnce();
		expect(torrentClientMock.getPhaseRegulator()).andReturn(regulatorMock).atLeastOnce();
		expect(regulatorMock.createInitialPhase(same(torrentClientMock), notNull())).andReturn(phaseMock).times(2);
		phaseMock.onPhaseEnter();
		expectLastCall().times(2);

		expect(executorServiceMock.scheduleAtFixedRate(notNull(), eq(0L), eq(250L), eq(TimeUnit.MILLISECONDS))).andReturn(futureMock).times(2);
		expect(executorServiceMock.scheduleAtFixedRate(notNull(), eq(1L), eq(10L), eq(TimeUnit.SECONDS))).andReturn(futureMock).times(2);
		expect(executorServiceMock.scheduleAtFixedRate(notNull(), eq(30L), eq(60L), eq(TimeUnit.SECONDS))).andReturn(futureMock).times(2);
		expect(executorServiceMock.scheduleAtFixedRate(notNull(), eq(10L), eq(30L), eq(TimeUnit.SECONDS))).andReturn(futureMock).times(2);

		replayAll();

		Metadata metadata = new Metadata.Builder()
				.setHash(DummyEntity.createUniqueTorrentHash())
				.build();

		Metadata metadataTwo = new Metadata.Builder()
				.setHash(DummyEntity.createUniqueTorrentHash(metadata.getHash()))
				.build();

		Torrent torrent = new Torrent.Builder()
				.setName("Test")
				.setMetadata(metadata)
				.setTorrentClient(torrentClientMock)
				.build();
		Torrent torrentTwo = new Torrent.Builder()
				.setName("Test Two")
				.setMetadata(metadataTwo)
				.setTorrentClient(torrentClientMock)
				.build();

		TorrentManager cut = new TorrentManager(trackerManager);
		cut.start(torrentClientMock);

		assertNotPresent("Torrent should not have been found yet", cut.getTorrent(torrent.getMetadata().getHash()));

		cut.addTorrent(torrent);

		assertPresent("Torrent should have been present", cut.getTorrent(torrent.getMetadata().getHash()));
		assertTrue("Collection should have contained torrent", cut.getTorrents().contains(torrent));

		cut.addTorrent(torrentTwo);

		assertEquals("Torrent should have been equal", torrent, cut.getTorrent(torrent.getMetadata().getHash()).get());
		assertEquals("Torrent two should have been equal", torrentTwo, cut.getTorrent(torrentTwo.getMetadata().getHash()).get());
		assertTrue("Collection should have contained torrent", cut.getTorrents().contains(torrent));
		assertTrue("Collection should have contained torrent two", cut.getTorrents().contains(torrentTwo));

		cut.removeTorrent(torrentTwo);
		assertFalse("Collection should not have contained torrent two", cut.getTorrents().contains(torrentTwo));

		verifyAll();
	}

	@Test
	public void testShutdownTorrent() {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		ScheduledExecutorService executorServiceMock = createMock(ScheduledExecutorService.class);
		IDownloadPhase phaseMock = createMock(IDownloadPhase.class);
		PhaseRegulator regulatorMock = createMock(PhaseRegulator.class);
		ScheduledFuture futureMock = createMock(ScheduledFuture.class);
		TrackerManager trackerManager = createMock(TrackerManager.class);

		expect(torrentClientMock.getExecutorService()).andReturn(executorServiceMock).atLeastOnce();
		expect(torrentClientMock.getPhaseRegulator()).andReturn(regulatorMock).atLeastOnce();
		expect(regulatorMock.createInitialPhase(same(torrentClientMock), notNull())).andReturn(phaseMock).times(2);
		phaseMock.onPhaseEnter();
		expectLastCall().times(2);

		expect(futureMock.cancel(eq(false))).andReturn(true).times(4);

		expect(executorServiceMock.scheduleAtFixedRate(notNull(), eq(0L), eq(250L), eq(TimeUnit.MILLISECONDS))).andReturn(futureMock).times(2);
		expect(executorServiceMock.scheduleAtFixedRate(notNull(), eq(1L), eq(10L), eq(TimeUnit.SECONDS))).andReturn(futureMock).times(2);
		expect(executorServiceMock.scheduleAtFixedRate(notNull(), eq(30L), eq(60L), eq(TimeUnit.SECONDS))).andReturn(futureMock).times(2);
		expect(executorServiceMock.scheduleAtFixedRate(notNull(), eq(10L), eq(30L), eq(TimeUnit.SECONDS))).andReturn(futureMock).times(2);

		replayAll();

		Metadata metadata = new Metadata.Builder()
				.setHash(DummyEntity.createUniqueTorrentHash())
				.build();

		Metadata metadataTwo = new Metadata.Builder()
				.setHash(DummyEntity.createUniqueTorrentHash(metadata.getHash()))
				.build();

		Torrent torrent = new Torrent.Builder()
				.setName("Test")
				.setMetadata(metadata)
				.setTorrentClient(torrentClientMock)
				.build();
		Torrent torrentTwo = new Torrent.Builder()
				.setName("Test Two")
				.setMetadata(metadataTwo)
				.setTorrentClient(torrentClientMock)
				.build();

		TorrentManager cut = new TorrentManager(trackerManager);
		cut.start(torrentClientMock);

		cut.addTorrent(torrent);
		cut.addTorrent(torrentTwo);

		cut.shutdownTorrent(torrentTwo);
		assertFalse("Collection should not have contained torrent two", cut.getTorrents().contains(torrentTwo));
		cut.shutdownTorrent(torrentTwo);

		verifyAll();
	}

	@Test
	public void testStartStopWithoutPeerConnector() throws Exception {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		TrackerManager trackerManager = createMock(TrackerManager.class);
		replayAll();

		TorrentManager cut = new TorrentManager(trackerManager);

		cut.start(torrentClientMock);

		LoopingRunnable peerIoRunnable = Whitebox.getInternalState(cut, "peerIoRunnable");
		assertNotNull("Peer IO runner should have been started.", peerIoRunnable);

		cut.stop();

		verifyAll();
		assertFalse("Peer IO runner should have been tasked to stop", isRunning(peerIoRunnable));
	}

	@Test
	public void testStartStopWithPeerConnector() throws Exception {
		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		TrackerManager trackerManager = createMock(TrackerManager.class);
		expect(torrentClientMock.getDownloadPort()).andReturn(DummyEntity.findAvailableTcpPort());
		replayAll();

		TorrentManager cut = new TorrentManager(trackerManager);


		cut.start(torrentClientMock);
		cut.enableConnectionAcceptor();

		LoopingRunnable peerIoRunnable = Whitebox.getInternalState(cut, "peerIoRunnable");
		LoopingRunnable peerConnectorRunnable = Whitebox.getInternalState(cut, "connectorRunnable");
		assertNotNull("Peer IO runner should have been started.", peerIoRunnable);
		assertNotNull("Peer connector runner should have been started.", peerConnectorRunnable);

		cut.stop();
		verifyAll();

		assertFalse("Peer IO runner should have been tasked to stop", isRunning(peerIoRunnable));
		assertFalse("Peer connector runner should have been tasked to stop", isRunning(peerConnectorRunnable));
	}

	private boolean isRunning(LoopingRunnable runnable) {
		return Whitebox.getInternalState(runnable, "keepRunning");
	}

}