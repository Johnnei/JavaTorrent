package org.johnnei.javatorrent.tracker;

import static org.easymock.EasyMock.and;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.newCapture;
import static org.easymock.EasyMock.notNull;
import static org.junit.Assert.assertEquals;

import java.net.DatagramSocket;

import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.test.Whitebox;
import org.johnnei.javatorrent.torrent.tracker.ITracker;
import org.johnnei.javatorrent.torrent.tracker.TrackerException;
import org.johnnei.javatorrent.torrent.tracker.TrackerManager;
import org.johnnei.javatorrent.tracker.udp.UdpTrackerSocket;
import org.johnnei.javatorrent.utils.CheckedBiFunction;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class UdpTrackerModuleTest extends EasyMockSupport {

	@Test
	public void testStaticOutputFunctions() {
		UdpTrackerModule cut = new UdpTrackerModule.Builder().build();

		assertEquals("No dependencies expected for this module", 0, cut.getDependsOn().size());
		assertEquals("Related BEP should be #15.", 15, cut.getRelatedBep());

		// onHandshake should do nothing with the peer, expect succes when it's passed as null
		cut.onPostHandshake(null);
	}

	@Test
	public void testConfigureTorrentClient() {
		UdpTrackerModule cut = new UdpTrackerModule.Builder().build();

		TorrentClient.Builder builderMock = createMock(TorrentClient.Builder.class);
		expect(builderMock.registerTrackerProtocol(eq("udp"), notNull())).andReturn(builderMock);
		replayAll();

		cut.configureTorrentClient(builderMock);

		verifyAll();
	}

	@Test
	public void testOnBuildAndShutdown() throws Exception {
		final int port = 27960;
		UdpTrackerModule cut = new UdpTrackerModule.Builder()
				.setPort(port)
				.build();

		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		TrackerManager trackerManager = createMock(TrackerManager.class);
		expect(torrentClientMock.getTrackerManager()).andStubReturn(trackerManager);
		replayAll();

		cut.onBuild(torrentClientMock);
		cut.onShutdown();

		verifyAll();

		assertEquals("Incorrect port stored", port, (int) Whitebox.getInternalState(cut, "trackerPort"));
		UdpTrackerSocket trackerSocket = Whitebox.getInternalState(cut, "socket");
		DatagramSocket socket = Whitebox.getInternalState(trackerSocket, "udpSocket");

		assertEquals("Iconrrect port being used", port, socket.getLocalPort());
	}

	@Test
	public void testTrackerFactory() throws Exception {
		UdpTrackerModule cut = new UdpTrackerModule.Builder().build();

		Capture<CheckedBiFunction<String, TorrentClient, ITracker, TrackerException>> supplierCapture = newCapture();

		TorrentClient torrentClientMock = createMock(TorrentClient.class);
		TorrentClient.Builder builderMock = createMock(TorrentClient.Builder.class);
		expect(builderMock.registerTrackerProtocol(eq("udp"), and(capture(supplierCapture), notNull()))).andReturn(builderMock);
		replayAll();

		// Obtain the factory instance
		cut.configureTorrentClient(builderMock);

		// Invoke the actual factory
		final String trackerUrl = "udp://localhost:80";
		UdpTracker tracker = (UdpTracker) supplierCapture.getValue().apply(trackerUrl, torrentClientMock);

		verifyAll();
		assertEquals("Incorrect name", "localhost", tracker.getName());
		assertEquals("Incorrect status", "Idle", tracker.getStatus());
		assertEquals("Incorrect port", 80, tracker.getSocketAddress().getPort());
	}

}
