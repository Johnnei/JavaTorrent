package org.johnnei.javatorrent;

import java.util.Collections;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;

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
import org.johnnei.javatorrent.test.TestUtils;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.tracker.IPeerConnector;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.anyLong;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(EasyMockRunner.class)
public class TorrentClientTest extends EasyMockSupport {

	@Test
	public void testBuilder() throws Exception {
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = createMock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = createMock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);
		ITracker trackerMock = createMock(ITracker.class);
		IMessage messageMock = createMock(IMessage.class);
		IModule moduleMock = createMock(IModule.class);
		Torrent torrentMock = createMock(Torrent.class);
		IDownloadPhase phaseMock = createMock(IDownloadPhase.class);
		peerConnectorMock.start();

		TorrentClient.Builder builder = new TorrentClient.Builder();

		trackerMock.addTorrent(same(torrentMock));
		expect(moduleMock.getDependsOn()).andReturn(Collections.emptyList());
		expect(moduleMock.getRelatedBep()).andReturn(3);
		moduleMock.configureTorrentClient(same(builder));
		moduleMock.onBuild(notNull());
		expect(phaseRegulatorMock.createInitialPhase(notNull(), notNull())).andReturn(phaseMock);
		phaseMock.onPhaseEnter();
		expect(executorServiceMock.scheduleAtFixedRate(notNull(), anyLong(), anyLong(), notNull())).andReturn(null).times(3);

		replayAll();
		TorrentClient torrentClient = builder
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> trackerMock)
				.registerMessage(15, () -> messageMock)
				.registerModule(moduleMock)
				.setDownloadPort(27960)
				.build();

		torrentClient.download(torrentMock, Collections.singleton("udp://localhost:80"));
		verifyAll();

		assertEquals("Incorrect connection degradation instance", connectionDegradationMock, torrentClient.getConnectionDegradation());
		assertEquals("Incorrect phase regulator instance", phaseRegulatorMock, torrentClient.getPhaseRegulator());
		assertEquals("Incorrect executor service instance", executorServiceMock, torrentClient.getExecutorService());
		assertEquals("Incorrect peer connector instance", peerConnectorMock, torrentClient.getPeerConnector());
		assertEquals("Incorrect download port", 27960, torrentClient.getDownloadPort());

		// The main BitTorrent protocol defines 8 messages, test if those are added by default.
		for (int i = 0; i < 8; i++) {
			assertNotNull(String.format("Missing message: %d", i), torrentClient.getMessageFactory().createById(i));
		}

		assertNotNull("Missing message: 15", torrentClient.getMessageFactory().createById(15));
		byte[] peerId = torrentClient.getPeerId();

		// Assert that the peer id is in format: -JTdddd-xxxxxxxxxxxx
		// The first 8 bytes are always readable ASCII characters.
		String clientIdentifier = new String(peerId, 0, 8);
		assertTrue("Incorrect client identifier in peer ID", Pattern.matches("-JT\\d{4}-", clientIdentifier));
		TestUtils.assertPresent("Missing module", torrentClient.getModule(moduleMock.getClass()));
		assertTrue("Missing module", torrentClient.getModules().contains(moduleMock));
	}

	@Test(expected = IllegalStateException.class)
	public void testRegisterModuleMissingDependency() {
		IModule moduleMock = createMock(IModule.class);

		expect(moduleMock.getDependsOn()).andReturn(Collections.singletonList(IModule.class));

		replayAll();

		new TorrentClient.Builder().registerModule(moduleMock);

		verifyAll();
	}

	@Test
	public void testGetExtensionBytesEnableSpecificBit() throws Exception {
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = createMock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = createMock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);
		peerConnectorMock.start();

		replayAll();

		TorrentClient torrentClient = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.enableExtensionBit(20)
				.build();

		verifyAll();

		byte[] extensionBytes = torrentClient.getExtensionBytes();
		byte[] expectedBytes = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x10, 0x00, 0x00 };
		assertEquals("Incorrect amount of extension byes", 8, extensionBytes.length);
		assertArrayEquals("Extension bit didn't get enabled correctly", expectedBytes, extensionBytes);
	}

	@Test
	public void testGetExtensionBytesEnableAllBits() throws Exception {
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = createMock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = createMock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);
		peerConnectorMock.start();

		replayAll();

		TorrentClient.Builder builder = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> null);

		// There are 8 extension bytes, enable them all
		for (int i = 0; i < 64; i++) {
			builder.enableExtensionBit(i);
		}

		byte[] extensionBytes = builder.build().getExtensionBytes();
		verifyAll();

		byte[] expectedBytes = {
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		};
		assertEquals("Incorrect amount of extension byes", 8, extensionBytes.length);
		assertArrayEquals("Extension bit didn't get enabled correctly", expectedBytes, extensionBytes);
	}

	@Test
	public void testCreateUniqueTransactionId() throws Exception {
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = createMock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = createMock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);
		peerConnectorMock.start();

		replayAll();

		TorrentClient cut = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.build();

		int id = cut.createUniqueTransactionId();
		int id2 = cut.createUniqueTransactionId();

		assertNotEquals("Duplicate transaction IDs", id, id2);
	}

	@Test
	public void testShutdown() throws Exception {
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = createMock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = createMock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);
		peerConnectorMock.start();
		peerConnectorMock.stop();

		executorServiceMock.shutdown();

		replayAll();

		TorrentClient cut = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.build();

		TorrentManager torrentManager = Whitebox.getInternalState(cut, TorrentManager.class);
		LoopingRunnable peerIoRunnable = Whitebox.getInternalState(torrentManager, "peerIoRunnable");

		assertTrue("Peer IO should have been invoked to start", Whitebox.getInternalState(peerIoRunnable, "keepRunning"));

		cut.shutdown();

		assertFalse("Peer IO should have been invoked to start", Whitebox.getInternalState(peerIoRunnable, "keepRunning"));

		verifyAll();
	}

	@Test
	public void testAddDiskJob() throws Exception {
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = createMock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = createMock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);
		IOManager ioManagerMock = createMock(IOManager.class);
		IDiskJob diskJobMock = createMock(IDiskJob.class);
		peerConnectorMock.start();

		ioManagerMock.addTask(same(diskJobMock));

		replayAll();

		TorrentClient cut = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.build();

		Whitebox.setInternalState(cut, ioManagerMock);

		cut.addDiskJob(diskJobMock);

		verifyAll();
	}

	@Test
	public void testAcceptIncomingConnections() throws Exception {
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = createMock(PhaseRegulator.class);
		ScheduledExecutorService executorServiceMock = createMock(ScheduledExecutorService.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);
		peerConnectorMock.start();

		replayAll();

		TorrentClient cut = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> null)
				.acceptIncomingConnections(true)
				.build();

		verifyAll();

		TorrentManager torrentManager = Whitebox.getInternalState(cut, TorrentManager.class);
		assertNotNull("Connector should have been attempted to start.", Whitebox.getInternalState(torrentManager, "connectorRunnable"));
	}
}