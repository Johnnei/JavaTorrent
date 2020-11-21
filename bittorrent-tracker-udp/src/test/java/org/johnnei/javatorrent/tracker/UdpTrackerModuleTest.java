package org.johnnei.javatorrent.tracker;

import java.net.DatagramSocket;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.TorrentClientSettings;
import org.johnnei.javatorrent.bittorrent.tracker.ITracker;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerException;
import org.johnnei.javatorrent.internal.tracker.udp.UdpTrackerSocket;
import org.johnnei.javatorrent.utils.CheckedBiFunction;

import static org.johnnei.javatorrent.test.DummyEntity.findAvailableUdpPort;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class UdpTrackerModuleTest {

	@Test
	public void testStaticOutputFunctions() {
		UdpTrackerModule cut = new UdpTrackerModule.Builder().build();

		assertEquals(0, cut.getDependsOn().size(), "No dependencies whened for this module");
		assertEquals(15, cut.getRelatedBep(), "Related BEP should be #15.");

		// onHandshake should do nothing with the peer, when succes when it's passed as null
		cut.onPostHandshake(null);
	}

	@Test
	public void testConfigureTorrentClient() {
		UdpTrackerModule cut = new UdpTrackerModule.Builder().build();

		TorrentClient.Builder builderMock = mock(TorrentClient.Builder.class);
		when(builderMock.registerTrackerProtocol(eq("udp"), notNull())).thenReturn(builderMock);

		cut.configureTorrentClient(builderMock);
	}

	@Test
	public void testOnBuildAndShutdown() throws Exception {
		final int port = findAvailableUdpPort();
		UdpTrackerModule cut = new UdpTrackerModule.Builder()
				.build();

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		TorrentClientSettings settingsMock = mock(TorrentClientSettings.class);
		when(torrentClientMock.getSettings()).thenReturn(settingsMock);

		try {
			cut.onBuild(torrentClientMock);
		} finally {
			// Clean up the socket after the test to prevent retaining a bound socket.
			cut.onShutdown();
		}

		verify(settingsMock, times(1)).getAcceptingPort();
	}

	@Test
	public void testTrackerFactory() throws Exception {
		UdpTrackerModule cut = new UdpTrackerModule.Builder().build();

		ArgumentCaptor<CheckedBiFunction<String, TorrentClient, ITracker, TrackerException>> supplierCapture = ArgumentCaptor.forClass(CheckedBiFunction.class);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		TorrentClient.Builder builderMock = mock(TorrentClient.Builder.class);
		when(builderMock.registerTrackerProtocol(eq("udp"), supplierCapture.capture())).thenReturn(builderMock);

		// Obtain the factory instance
		cut.configureTorrentClient(builderMock);

		// Invoke the actual factory
		final String trackerUrl = "udp://localhost:80";
		UdpTracker tracker = (UdpTracker) supplierCapture.getValue().apply(trackerUrl, torrentClientMock);

		assertEquals("localhost", tracker.getName(), "Incorrect name");
		assertEquals("Idle", tracker.getStatus(), "Incorrect status");
		assertEquals(80, tracker.getSocketAddress().getPort(), "Incorrect port");
	}

}
