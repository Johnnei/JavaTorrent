package org.johnnei.javatorrent.internal.torrent;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.network.connector.NioConnectionAcceptor;
import org.johnnei.javatorrent.internal.tracker.TrackerManager;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link TorrentManager}
 */
public class TorrentManagerTest {

	@Test
	public void testAddRemoveGetTorrent() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		IDownloadPhase phaseMock = mock(IDownloadPhase.class);
		PhaseRegulator regulatorMock = mock(PhaseRegulator.class);
		ScheduledFuture futureMock = mock(ScheduledFuture.class);
		TrackerManager trackerManager = mock(TrackerManager.class);

		when(torrentClientMock.getExecutorService()).thenReturn(executorServiceMock);
		when(torrentClientMock.getPhaseRegulator()).thenReturn(regulatorMock);
		when(regulatorMock.createInitialPhase(same(torrentClientMock), notNull())).thenReturn(phaseMock);

		when(executorServiceMock.scheduleAtFixedRate(notNull(), eq(0L), eq(250L), eq(TimeUnit.MILLISECONDS))).thenReturn(futureMock);
		when(executorServiceMock.scheduleAtFixedRate(notNull(), eq(1L), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(futureMock);
		when(executorServiceMock.scheduleAtFixedRate(notNull(), eq(30L), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(futureMock);
		when(executorServiceMock.scheduleAtFixedRate(notNull(), eq(10L), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(futureMock);

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
		assertTrue(cut.getTorrents().contains(torrent), "Collection should have contained torrent");

		cut.addTorrent(torrentTwo);

		assertEquals(torrent, cut.getTorrent(torrent.getMetadata().getHash()).get(), "Torrent should have been equal");
		assertEquals(torrentTwo, cut.getTorrent(torrentTwo.getMetadata().getHash()).get(), "Torrent two should have been equal");
		assertTrue(cut.getTorrents().contains(torrent), "Collection should have contained torrent");
		assertTrue(cut.getTorrents().contains(torrentTwo), "Collection should have contained torrent two");

		cut.removeTorrent(torrentTwo);
		assertFalse(cut.getTorrents().contains(torrentTwo), "Collection should not have contained torrent two");

		verify(phaseMock, times(2)).onPhaseEnter();
	}

	@Test
	public void testShutdownTorrent() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		IDownloadPhase phaseMock = mock(IDownloadPhase.class);
		PhaseRegulator regulatorMock = mock(PhaseRegulator.class);
		ScheduledFuture futureMock = mock(ScheduledFuture.class);
		TrackerManager trackerManager = mock(TrackerManager.class);

		when(torrentClientMock.getExecutorService()).thenReturn(executorServiceMock);
		when(torrentClientMock.getPhaseRegulator()).thenReturn(regulatorMock);
		when(regulatorMock.createInitialPhase(same(torrentClientMock), notNull())).thenReturn(phaseMock);

		when(futureMock.cancel(eq(false))).thenReturn(true);

		when(executorServiceMock.scheduleAtFixedRate(notNull(), eq(0L), eq(250L), eq(TimeUnit.MILLISECONDS))).thenReturn(futureMock);
		when(executorServiceMock.scheduleAtFixedRate(notNull(), eq(1L), eq(10L), eq(TimeUnit.SECONDS))).thenReturn(futureMock);
		when(executorServiceMock.scheduleAtFixedRate(notNull(), eq(30L), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(futureMock);
		when(executorServiceMock.scheduleAtFixedRate(notNull(), eq(10L), eq(30L), eq(TimeUnit.SECONDS))).thenReturn(futureMock);

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
		assertFalse(cut.getTorrents().contains(torrentTwo), "Collection should not have contained torrent two");
		cut.shutdownTorrent(torrentTwo);

		verify(phaseMock, times(2)).onPhaseEnter();
	}

	@Test
	public void testStartStopWithoutPeerConnector() {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		TrackerManager trackerManager = mock(TrackerManager.class);

		TorrentManager cut = new TorrentManager(trackerManager);

		cut.start(torrentClientMock);

		NioConnectionAcceptor connectionAcceptor = Whitebox.getInternalState(cut, "connectionAcceptor");
		assertThat("Peer IO runner should have been started.", connectionAcceptor, nullValue());

		cut.stop();
	}

	@Test
	public void testStartStopWithPeerConnector() throws Exception {
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		TrackerManager trackerManager = mock(TrackerManager.class);
		when(torrentClientMock.getDownloadPort()).thenReturn(DummyEntity.findAvailableTcpPort());
		ScheduledExecutorService executor = mock(ScheduledExecutorService.class);

		when(torrentClientMock.getExecutorService()).thenReturn(executor);

		TorrentManager cut = new TorrentManager(trackerManager);

		ScheduledFuture task = mock(ScheduledFuture.class);
		when(executor.scheduleWithFixedDelay(notNull(), eq(50L), eq(100L), eq(TimeUnit.MILLISECONDS))).thenReturn(task);

		cut.start(torrentClientMock);
		cut.enableConnectionAcceptor();

		NioConnectionAcceptor connectionAcceptor = Whitebox.getInternalState(cut, "connectionAcceptor");
		assertThat("Peer IO runner should have been started.", connectionAcceptor, notNullValue());

		verify(executor).scheduleWithFixedDelay(notNull(), eq(50L), eq(100L), eq(TimeUnit.MILLISECONDS));

		cut.stop();

		verify(task).cancel(false);
	}

	@Test
	public void testGetPeerState() {
		TrackerManager trackerManager = mock(TrackerManager.class);
		TorrentClient torrentClient = mock(TorrentClient.class);
		ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
		PhaseRegulator phaseRegulator = mock(PhaseRegulator.class);
		IDownloadPhase downloadPhase = mock(IDownloadPhase.class);
		Torrent torrent = mock(Torrent.class);

		when(torrentClient.getExecutorService()).thenReturn(executor);
		when(torrentClient.getPhaseRegulator()).thenReturn(phaseRegulator);
		when(phaseRegulator.createInitialPhase(torrentClient, torrent)).thenReturn(downloadPhase);

		TorrentManager cut = new TorrentManager(trackerManager);

		cut.start(torrentClient);
		cut.addTorrent(torrent);

		assertNotNull(cut.getPeerStateAccess(torrent));
	}

}
