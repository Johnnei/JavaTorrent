package org.johnnei.javatorrent;

import java.util.Collections;
import java.util.concurrent.ExecutorService;

import org.johnnei.javatorrent.module.IModule;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.algos.peermanager.IPeerManager;
import org.johnnei.javatorrent.tracker.IPeerConnector;
import org.johnnei.javatorrent.bittorrent.tracker.ITracker;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(EasyMockRunner.class)
public class TorrentClientTest extends EasyMockSupport {

	@Test
	public void testBuilder() throws Exception {
		ConnectionDegradation connectionDegradationMock = createMock(ConnectionDegradation.class);
		PhaseRegulator phaseRegulatorMock = createMock(PhaseRegulator.class);
		ExecutorService executorServiceMock = createMock(ExecutorService.class);
		IPeerManager peerManagerMock = createMock(IPeerManager.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);
		ITracker trackerMock = createMock(ITracker.class);
		IMessage messageMock = createMock(IMessage.class);
		IModule moduleMock = createMock(IModule.class);
		Torrent torrentMock = createMock(Torrent.class);

		TorrentClient.Builder builder = new TorrentClient.Builder();

		trackerMock.addTorrent(same(torrentMock));
		expect(moduleMock.getDependsOn()).andReturn(Collections.emptyList());
		expect(moduleMock.getRelatedBep()).andReturn(3);
		moduleMock.configureTorrentClient(same(builder));
		moduleMock.onBuild(notNull());

		replayAll();
		TorrentClient torrentClient = builder
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerManager(peerManagerMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> trackerMock)
				.registerMessage(15, () -> messageMock)
				.registerModule(moduleMock)
				.setDownloadPort(27960)
				.build();

		torrentClient.getTrackerManager().addTorrent(torrentMock, "udp://localhost:80");
		verifyAll();

		assertEquals("Incorrect connection degradation instance", connectionDegradationMock, torrentClient.getConnectionDegradation());
		assertEquals("Incorrect phase regulator instance", phaseRegulatorMock, torrentClient.getPhaseRegulator());
		assertEquals("Incorrect executor service instance", executorServiceMock, torrentClient.getExecutorService());
		assertEquals("Incorrect peer manager instance", peerManagerMock, torrentClient.getPeerManager());
		assertEquals("Incorrect peer connector instance", peerConnectorMock, torrentClient.getPeerConnector());
		assertEquals("Incorrect download port", 27960, torrentClient.getDownloadPort());

		// The main BitTorrent protocol defines 8 messages, test if those are added by default.
		for (int i = 0; i < 8; i++) {
			assertNotNull(String.format("Missing message: %d", i), torrentClient.getMessageFactory().createById(i));
		}

		assertNotNull("Missing message: 15", torrentClient.getMessageFactory().createById(15));
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
		ExecutorService executorServiceMock = createMock(ExecutorService.class);
		IPeerManager peerManagerMock = createMock(IPeerManager.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);

		replayAll();

		TorrentClient torrentClient = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerManager(peerManagerMock)
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
		ExecutorService executorServiceMock = createMock(ExecutorService.class);
		IPeerManager peerManagerMock = createMock(IPeerManager.class);
		IPeerConnector peerConnectorMock = createMock(IPeerConnector.class);

		replayAll();

		TorrentClient.Builder builder = new TorrentClient.Builder()
				.setConnectionDegradation(connectionDegradationMock)
				.setPhaseRegulator(phaseRegulatorMock)
				.setExecutorService(executorServiceMock)
				.setPeerManager(peerManagerMock)
				.setPeerConnector((t) -> peerConnectorMock)
				.registerTrackerProtocol("udp", (url, client) -> null);

		// There are 8 extension bytes, enable them all
		for (int i = 0; i < 64; i++) {
			builder.enableExtensionBit(i);
		}

		verifyAll();

		byte[] extensionBytes = builder.build().getExtensionBytes();
		byte[] expectedBytes = {
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
		};
		assertEquals("Incorrect amount of extension byes", 8, extensionBytes.length);
		assertArrayEquals("Extension bit didn't get enabled correctly", expectedBytes, extensionBytes);
	}
}