package org.johnnei.javatorrent.utp;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.internal.network.socket.ISocket;
import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.UtpSocketRegistration;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;
import org.johnnei.javatorrent.network.ConnectionDegradation;
import org.johnnei.javatorrent.phases.PhaseData;
import org.johnnei.javatorrent.phases.PhaseRegulator;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.tracker.PeerConnector;
import org.johnnei.javatorrent.tracker.UncappedDistributor;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.powermock.reflect.Whitebox.getInternalState;

/**
 * Tests the ability to cleanly connect and disconnect on uTP sockets.
 */
public class ConnectUtpSocketIT {

	@Rule
	public Timeout timeout = new Timeout(30, TimeUnit.SECONDS);

	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectUtpSocketIT.class);

	private TorrentClient localClient;

	private TorrentClient remoteClient;

	private TorrentClient createTorrentClient(UtpModule utpModule, int port) throws Exception {
		return new TorrentClient.Builder()
				// Disable incoming TCP connections, only expect UTP
				.acceptIncomingConnections(false)
				.setPeerDistributor(UncappedDistributor::new)
				.registerModule(utpModule)
				.setConnectionDegradation(new ConnectionDegradation.Builder()
						.registerDefaultConnectionType(utpModule.getUtpSocketClass(), utpModule.createSocketFactory(), Optional.empty())
						.build())
				.setDownloadPort(port)
				.setExecutorService(Executors.newScheduledThreadPool(2))
				.setPeerConnector(PeerConnector::new)
				.registerTrackerProtocol("stub", (s, torrentClient) -> null)
				.setPhaseRegulator(new PhaseRegulator.Builder()
						.registerInitialPhase(PhaseData.class, PhaseData::new, Optional.empty())
						.build()
				).build();
	}

	@After
	public void tearDown() {
		if (localClient != null) {
			localClient.shutdown();
		}

		if (remoteClient != null) {
			remoteClient.shutdown();
		}
	}

	@Test
	public void testConnectThenDisconnect() throws Exception {
		LOGGER.info("Preparing test environment.");

		UtpModule localUtpModule = new UtpModule();
		localClient = createTorrentClient(localUtpModule, DummyEntity.findAvailableUdpPort());

		UtpModule remoteUtpModule = new UtpModule();
		remoteClient = createTorrentClient(remoteUtpModule, DummyEntity.findAvailableUdpPort());

		ISocket localSocket = localUtpModule.createSocketFactory().get();

		UtpMultiplexer remoteMultiplexer = getInternalState(remoteUtpModule, UtpMultiplexer.class);

		assertNotNull("Failed to putIfAbsent UtpMultiplexer instance on remote UtpModule", remoteMultiplexer);

		LOGGER.info("Connecting sockets.");
		localSocket.connect(new InetSocketAddress("localhost", remoteClient.getDownloadPort()));

		UtpSocketImpl localSocketImpl = getInternalState(localSocket, UtpSocketImpl.class);
		assertNotNull("Failed to putIfAbsent the UtpSocketImpl instance on local socket", localSocketImpl);

		assertEquals("Connection state on local socket must have transitioned to connected.", ConnectionState.CONNECTED, localSocketImpl.getConnectionState());

		Map<Short, UtpSocketRegistration> localSockets = getInternalState(getInternalState(localUtpModule, UtpMultiplexer.class), "utpSockets");
		Map<Short, UtpSocketRegistration> remoteSockets = getInternalState(remoteMultiplexer, "utpSockets");

		UtpSocketRegistration remoteRegistration = remoteSockets.get(localSocketImpl.getSendingConnectionId());

		assertNotNull("Remote socket is registered on wrong connection id", remoteRegistration);
		UtpSocketImpl remoteSocketImpl = remoteRegistration.getSocket();

		assertEquals("Remote socket must still be in the connecting state", ConnectionState.CONNECTING, remoteSocketImpl.getConnectionState());

		LOGGER.info("Connecting endpoint established connection, sending data to trigger connected state on remote end.");
		localSocket.getOutputStream().write(42);
		localSocket.flush();

		// This transition might take a few seconds due to the physical connection usage.
		await("Remote socket failed to transition to connect.")
				.atMost(5, TimeUnit.SECONDS)
				.until(() -> remoteSocketImpl.getConnectionState() == ConnectionState.CONNECTED);

		assertEquals("Data packet was corrupted on remote socket", 42, remoteSocketImpl.getInputStream().read());
		LOGGER.info("Testing if local socket can receive data.");

		remoteSocketImpl.getOutputStream().write(24);
		remoteSocketImpl.getOutputStream().flush();

		assertEquals("Data packet was corrupted on local socket", 24, localSocket.getInputStream().read());

		LOGGER.info("Confirmed that connection was established. Closing connection.");
		localSocket.close();

		assertEquals("Connection state must have transitioned on local socket", ConnectionState.DISCONNECTING, localSocketImpl.getConnectionState());
		await("Remote socket failed to transition on disconnect.")
				.atMost(5, TimeUnit.SECONDS)
				.until(() -> remoteSocketImpl.getConnectionState() == ConnectionState.DISCONNECTING);

		LOGGER.info("Close has been triggered, awaiting that sockets clean themselves up.");
		await("Local socket transition to closed")
				.atMost(5, TimeUnit.SECONDS)
				.until(() -> localSocketImpl.getConnectionState() == ConnectionState.CLOSED);
		await("Remote socket transition to closed")
				.atMost(5, TimeUnit.SECONDS)
				.until(() -> remoteSocketImpl.getConnectionState() == ConnectionState.CLOSED);

		await("Local socket registration clean up")
				.atMost(5, TimeUnit.SECONDS)
				.until(() -> localSockets.size() == 0);

		await("Remote socket registration clean up")
				.atMost(5, TimeUnit.SECONDS)
				.until(() -> remoteSockets.size() == 0);
	}

}
