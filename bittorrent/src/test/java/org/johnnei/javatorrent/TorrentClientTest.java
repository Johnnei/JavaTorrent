package org.johnnei.javatorrent;

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.async.LoopingRunnable;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.disk.IDiskJob;
import org.johnnei.javatorrent.internal.disk.IOManager;
import org.johnnei.javatorrent.internal.torrent.TorrentManager;
import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.phases.IDownloadPhase;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.requests.IRequestLimiter;
import org.johnnei.javatorrent.tracker.IPeerConnector;
import org.johnnei.javatorrent.tracker.IPeerDistributor;

import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TorrentClientTest {

	@Test
	public void testBuilder() throws Exception {
		ConnectionDegradation connectionDegradationMock = mock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = mock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = mock(IPeerConnector.class);
		ITracker trackerMock = mock(ITracker.class);
		IMessage messageMock = mock(IMessage.class);
		IModule moduleMock = mock(IModule.class);
		Torrent torrentMock = mock(Torrent.class);
		IDownloadPhase phaseMock = mock(IDownloadPhase.class);
		IPeerDistributor peerDistributorMock = mock(IPeerDistributor.class);
		IRequestLimiter requestLimiterMock = mock(IRequestLimiter.class);

		TorrentClient.Builder builder = new TorrentClient.Builder();

		when(moduleMock.getDependsOn()).thenReturn(Collections.emptyList());
		when(moduleMock.getRelatedBep()).thenReturn(3);
		when(phaseRegulatorMock.createInitialPhase(notNull(), notNull())).thenReturn(phaseMock);
		when(executorServiceMock.scheduleAtFixedRate(notNull(), anyLong(), anyLong(), notNull())).thenReturn(null);

		TorrentClient torrentClient = builder
				.setConnectionDegradation(connectionDegradationMock)
				.setPeerDistributor(tc -> peerDistributorMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setRequestLimiter(requestLimiterMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> trackerMock)
				.registerMessage(15, () -> messageMock)
				.registerModule(moduleMock)
				.setDownloadPort(27960)
				.build();

		torrentClient.download(torrentMock, Collections.singleton("udp://localhost:80"));

		verify(peerConnectorMock).start();
		verify(trackerMock).addTorrent(same(torrentMock));
		verify(moduleMock).configureTorrentClient(same(builder));
		verify(moduleMock).onBuild(notNull());
		verify(phaseMock).onPhaseEnter();

		assertEquals(connectionDegradationMock, torrentClient.getConnectionDegradation(), "Incorrect connection degradation instance");
		assertEquals(phaseRegulatorMock, torrentClient.getPhaseRegulator(), "Incorrect phase regulator instance");
		assertEquals(executorServiceMock, torrentClient.getExecutorService(), "Incorrect executor service instance");
		assertEquals(peerConnectorMock, torrentClient.getPeerConnector(), "Incorrect peer connector instance");
		assertEquals(peerDistributorMock, torrentClient.getPeerDistributor(), "Incorrect peer distributor instance");
		assertEquals(27960, (Object) torrentClient.getDownloadPort(), "Incorrect download port");

		// The main BitTorrent protocol defines 8 messages, test if those are added by default.
		for (int i = 0; i < 8; i++) {
			assertNotNull(torrentClient.getMessageFactory().createById(i), String.format("Missing message: %d", i));
		}

		assertNotNull(torrentClient.getMessageFactory().createById(15), "Missing message: 15");
		byte[] peerId = torrentClient.getPeerId();

		// Assert that the peer id is in format: -JTdddd-xxxxxxxxxxxx
		// The first 8 bytes are always readable ASCII characters.
		String clientIdentifier = new String(peerId, 0, 8);
		assertTrue(Pattern.matches("-JT\\d{4}-", clientIdentifier), "Incorrect client identifier in peer ID: " + clientIdentifier);
		assertPresent("Missing module", torrentClient.getModule(moduleMock.getClass()));
		assertTrue(torrentClient.getModules().contains(moduleMock), "Missing module");
	}

	@Test
	public void testRegisterModuleMissingDependency() {
		IModule moduleMock = mock(IModule.class);

		when(moduleMock.getDependsOn()).thenReturn(Collections.singletonList(IModule.class));

		assertThrows(IllegalStateException.class, () -> new TorrentClient.Builder().registerModule(moduleMock));
	}

	@Test
	public void testGetExtensionBytesEnableSpecificBit() throws Exception {
		ConnectionDegradation connectionDegradationMock = mock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = mock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = mock(IPeerConnector.class);
		IPeerDistributor peerDistributor = mock(IPeerDistributor.class);
		IRequestLimiter requestLimiterMock = mock(IRequestLimiter.class);

		TorrentClient torrentClient = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setRequestLimiter(requestLimiterMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.setPeerDistributor(tc -> peerDistributor)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.enableExtensionBit(20)
				.build();
		
		verify(peerConnectorMock).start();

		byte[] extensionBytes = torrentClient.getExtensionBytes();
		byte[] whenedBytes = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00 };
		assertEquals(8, (Object) extensionBytes.length, "Incorrect amount of extension byes");
		assertArrayEquals(whenedBytes, extensionBytes, "Extension bit didn't get enabled correctly");
	}

	@Test
	public void testGetExtensionBytesEnableAllBits() throws Exception {
		ConnectionDegradation connectionDegradationMock = mock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = mock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = mock(IPeerConnector.class);
		IPeerDistributor peerDistributorMock = mock(IPeerDistributor.class);
		IRequestLimiter requestLimiter = mock(IRequestLimiter.class);

		TorrentClient.Builder builder = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setRequestLimiter(requestLimiter)
				.setPeerConnector((t) -> peerConnectorMock)
				.setPeerDistributor(tc -> peerDistributorMock)
				.registerTrackerProtocol("udp", (url, client) -> null);

		// There are 8 extension bytes, enable them all
		for (int i = 0; i < 64; i++) {
			builder.enableExtensionBit(i);
		}

		byte[] extensionBytes = builder.build().getExtensionBytes();

		verify(peerConnectorMock).start();

		byte[] whenedBytes = {
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		};
		assertEquals(8, (Object) extensionBytes.length, "Incorrect amount of extension byes");
		assertArrayEquals(whenedBytes, extensionBytes, "Extension bit didn't get enabled correctly");
	}

	@Test
	public void testCreateUniqueTransactionId() throws Exception {
		ConnectionDegradation connectionDegradationMock = mock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = mock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = mock(IPeerConnector.class);
		IPeerDistributor peerDistributorMock = mock(IPeerDistributor.class);
		IRequestLimiter requestLimiter = mock(IRequestLimiter.class);

		TorrentClient cut = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setRequestLimiter(requestLimiter)
				.setPeerConnector((t) -> peerConnectorMock)
				.setPeerDistributor(tc -> peerDistributorMock)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.build();

		int id = cut.createUniqueTransactionId();
		int id2 = cut.createUniqueTransactionId();
		
		verify(peerConnectorMock).start();

		assertNotEquals(id, id2, "Duplicate transaction IDs");
	}

	@Test
	public void testShutdown() throws Exception {
		ConnectionDegradation connectionDegradationMock = mock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = mock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = mock(IPeerConnector.class);
		IPeerDistributor peerDistributorMock = mock(IPeerDistributor.class);
		IRequestLimiter requestLimiterMock = mock(IRequestLimiter.class);
		IModule moduleMock = mock(IModule.class);

		when(moduleMock.getDependsOn()).thenReturn(Collections.emptyList());
		when(moduleMock.getRelatedBep()).thenReturn(3);

		TorrentClient cut = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setRequestLimiter(requestLimiterMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.setPeerDistributor(tc -> peerDistributorMock)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.registerModule(moduleMock)
				.build();

		TorrentManager torrentManager = Whitebox.getInternalState(cut, TorrentManager.class);
		LoopingRunnable peerIoRunnable = Whitebox.getInternalState(torrentManager, "peerIoRunnable");

		assertTrue(Whitebox.<Boolean>getInternalState(peerIoRunnable, "keepRunning"), "Peer IO should have been invoked to start");

		cut.shutdown();

		verify(moduleMock).configureTorrentClient(any());
		verify(moduleMock).onBuild(any());
		verify(moduleMock).onShutdown();

		verify(peerConnectorMock).start();
		verify(peerConnectorMock).stop();

		verify(executorServiceMock).shutdown();

		assertFalse(Whitebox.<Boolean>getInternalState(peerIoRunnable, "keepRunning"), "Peer IO should have been invoked to start");
	}

	@Test
	public void testAddDiskJob() throws Exception {
		ConnectionDegradation connectionDegradationMock = mock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = mock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = mock(IPeerConnector.class);
		IPeerDistributor peerDistributor = mock(IPeerDistributor.class);
		IRequestLimiter requestLimiterMock = mock(IRequestLimiter.class);
		IOManager ioManagerMock = mock(IOManager.class);
		IDiskJob diskJobMock = mock(IDiskJob.class);

		TorrentClient cut = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setRequestLimiter(requestLimiterMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.setPeerDistributor(tc -> peerDistributor)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.build();

		Whitebox.setInternalState(cut, ioManagerMock);

		cut.addDiskJob(diskJobMock);

		verify(peerConnectorMock).start();
		verify(ioManagerMock).addTask(same(diskJobMock));
	}

	@Test
	public void testAcceptIncomingConnections() throws Exception {
		ConnectionDegradation connectionDegradationMock = mock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = mock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = mock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = mock(IPeerConnector.class);
		IPeerDistributor peerDistributorMock = mock(IPeerDistributor.class);
		IRequestLimiter requestLimiterMock = mock(IRequestLimiter.class);

		TorrentClient cut = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setRequestLimiter(requestLimiterMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.setPeerDistributor(tc -> peerDistributorMock)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.acceptIncomingConnections(true)
				.build();

		verify(peerConnectorMock).start();

		TorrentManager torrentManager = Whitebox.getInternalState(cut, TorrentManager.class);
		assertNotNull(Whitebox.getInternalState(torrentManager, "connectorRunnable"), "Connector should have been attempted to start.");
	}
}
