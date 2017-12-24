package org.johnnei.javatorrent.tracker.http;

import java.net.InetSocketAddress;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.VerificationException;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedInteger;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedList;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedMap;
import org.johnnei.javatorrent.bittorrent.encoding.BencodedString;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.internal.tracker.http.HttpTracker;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.ExecutorServiceMock;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.tracker.IPeerConnector;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link HttpTracker}
 */
public class HttpTrackerIT {

	public static final Logger LOGGER = LoggerFactory.getLogger(HttpTrackerIT.class);

	private WireMockServer wireMock = new WireMockServer(options().dynamicPort());

	// @formatter:off
	private final byte[] torrentHash = new byte[] {
		(byte) 0x92,        0x28,        0x62, (byte) 0x85,        0x04,
		(byte) 0xcc,        0x40, (byte) 0xef, (byte) 0xa5,        0x7b,
		(byte) 0xf3, (byte) 0x8e, (byte) 0x85, (byte) 0xc9, (byte) 0xe3,
		(byte) 0xbd,        0x2c,        0x57,        0x2b,        0x5b
	};

	private final byte[] peerId = new byte [] {
		       0x58, 0x58,        0x58,        0x58,        0x58,
		       0x58, 0x58,        0x58,        0x58,        0x58,
		       0x58, 0x58,        0x58,        0x58, (byte) 0x58,
		(byte) 0x58, 0x58, (byte) 0x58, (byte) 0x58,        0x58
	};
	// @formatter:on

	@AfterEach
	public void tearDown() {
		try {
			List<LoggedRequest> nearMisses = wireMock.findAllUnmatchedRequests();
			if (!nearMisses.isEmpty()) {
				throw VerificationException.forUnmatchedRequests(nearMisses);
			}
		} finally {
			wireMock.stop();
		}
	}

	@BeforeEach
	public void setUp() {
		wireMock.start();
		WireMock.configureFor("localhost", wireMock.port());
	}

	@Test
	public void testAnnounce() throws Exception {
		BencodedMap announceResult = new BencodedMap();
		announceResult.put("interval", new BencodedInteger(15_000));

		BencodedList peerList = new BencodedList();

		BencodedMap peerWithoutHost = new BencodedMap();
		peerWithoutHost.put("peer id", new BencodedString(peerId));
		peerWithoutHost.put("port", new BencodedInteger(123));

		BencodedMap peerWithoutPort = new BencodedMap();
		peerWithoutPort.put("peer id", new BencodedString(peerId));
		peerWithoutPort.put("ip", new BencodedString("127.0.0.1"));

		BencodedMap peerWithoutId = new BencodedMap();
		peerWithoutId.put("ip", new BencodedString("127.0.0.1"));
		peerWithoutId.put("port", new BencodedInteger(123));

		peerList.add(peerWithoutHost);
		peerList.add(peerWithoutPort);
		peerList.add(peerWithoutId);

		announceResult.put("peers", peerList);
		OutStream outStream = new OutStream();
		outStream.write(announceResult.serialize());

		final String url = String.format("http://localhost:%d/announce", wireMock.port());

		WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/announce"))
			// Can't match the exact requests as jetty parses the incorrect UTF-8.
			.withQueryParam("info_hash", WireMock.matching(".*"))
			.withQueryParam("peer_id", WireMock.matching(".*"))
			.withQueryParam("port", WireMock.equalTo("27960"))
			.withQueryParam("uploaded", WireMock.equalTo("0"))
			.withQueryParam("downloaded", WireMock.equalTo("0"))
			.withQueryParam("left", WireMock.equalTo("0"))
			.withQueryParam("compact", WireMock.equalTo("0"))
			.withQueryParam("event", WireMock.equalTo("started"))
			.willReturn(WireMock.aResponse().withBody(outStream.toByteArray()))
		);

		IPeerConnector peerConnectorMock = mock(IPeerConnector.class);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getPeerId()).thenReturn(peerId);
		when(torrentClientMock.getDownloadPort()).thenReturn(27960);
		when(torrentClientMock.getExecutorService()).thenReturn(new ExecutorServiceMock());
		when(torrentClientMock.getPeerConnector()).thenReturn(peerConnectorMock);

		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getHash()).thenReturn(torrentHash);

		HttpTracker cut = new HttpTracker.Builder()
			.setTorrentClient(torrentClientMock)
			.setUrl(url)
			.build();

		cut.addTorrent(torrentMock);
		cut.announce(torrentMock);

		ArgumentCaptor<PeerConnectInfo> connectInfo = ArgumentCaptor.forClass(PeerConnectInfo.class);
		verify(peerConnectorMock).enqueuePeer(connectInfo.capture());

		assertEquals(InetSocketAddress.createUnresolved("127.0.0.1", 123), connectInfo.getValue().getAddress(), "Incorrect connect info: host");
		assertEquals(torrentMock, connectInfo.getValue().getTorrent(), "Incorrect connect info: torrent");
		assertEquals("Idle", cut.getStatus(), "Status should have returned to idle");
		assertEquals(15_000, cut.getAnnounceInterval(), "Incorrect interval");
	}

	@Test
	public void testAnnounceCompleted() throws Exception {
		BencodedMap announceResult = new BencodedMap();
		announceResult.put("interval", new BencodedInteger(15_000));

		announceResult.put("peers", new BencodedList());
		OutStream outStream = new OutStream();
		outStream.write(announceResult.serialize());

		final String url = String.format("http://localhost:%d/announce", wireMock.port());

		WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/announce"))
			// Can't match the exact requests as jetty parses the incorrect UTF-8.
			.withQueryParam("info_hash", WireMock.matching(".*"))
			.withQueryParam("peer_id", WireMock.matching(".*"))
			.withQueryParam("port", WireMock.equalTo("27960"))
			.withQueryParam("uploaded", WireMock.equalTo("0"))
			.withQueryParam("downloaded", WireMock.equalTo("0"))
			.withQueryParam("left", WireMock.equalTo("0"))
			.withQueryParam("compact", WireMock.equalTo("0"))
			.withQueryParam("event", WireMock.equalTo("completed"))
			.willReturn(WireMock.aResponse().withBody(outStream.toByteArray()))
		);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getPeerId()).thenReturn(peerId);
		when(torrentClientMock.getDownloadPort()).thenReturn(27960);
		when(torrentClientMock.getExecutorService()).thenReturn(new ExecutorServiceMock());

		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getHash()).thenReturn(torrentHash);

		HttpTracker cut = new HttpTracker.Builder()
			.setTorrentClient(torrentClientMock)
			.setUrl(url)
			.build();

		cut.addTorrent(torrentMock);

		assertTrue(cut.hasTorrent(torrentMock), "Add of torrent failed");

		cut.getInfo(torrentMock).get().setEvent(TrackerEvent.EVENT_COMPLETED);
		cut.announce(torrentMock);

		assertEquals(15_000, cut.getAnnounceInterval(), "Incorrect interval");
		assertEquals("Idle", cut.getStatus(), "Status should have returned to idle");
		verify(torrentClientMock, never()).getPeerConnector();
	}

	@Test
	public void testAnnounceNoEvent() throws Exception {
		BencodedMap announceResult = new BencodedMap();
		announceResult.put("interval", new BencodedInteger(15_000));

		announceResult.put("peers", new BencodedList());
		OutStream outStream = new OutStream();
		outStream.write(announceResult.serialize());

		final String url = String.format("http://localhost:%d/announce", wireMock.port());

		WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/announce"))
			// Can't match the exact requests as jetty parses the incorrect UTF-8.
			.withQueryParam("info_hash", WireMock.matching(".*"))
			.withQueryParam("peer_id", WireMock.matching(".*"))
			.withQueryParam("port", WireMock.equalTo("27960"))
			.withQueryParam("uploaded", WireMock.equalTo("0"))
			.withQueryParam("downloaded", WireMock.equalTo("0"))
			.withQueryParam("left", WireMock.equalTo("0"))
			.withQueryParam("compact", WireMock.equalTo("0"))
			.withQueryParam("event", WireMock.absent())
			.willReturn(WireMock.aResponse().withBody(outStream.toByteArray()))
		);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getPeerId()).thenReturn(peerId);
		when(torrentClientMock.getDownloadPort()).thenReturn(27960);
		when(torrentClientMock.getExecutorService()).thenReturn(new ExecutorServiceMock());

		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getHash()).thenReturn(torrentHash);

		HttpTracker cut = new HttpTracker.Builder()
			.setTorrentClient(torrentClientMock)
			.setUrl(url)
			.build();

		cut.addTorrent(torrentMock);

		assertTrue(cut.hasTorrent(torrentMock), "Add of torrent failed");

		cut.getInfo(torrentMock).get().setEvent(TrackerEvent.EVENT_NONE);
		cut.announce(torrentMock);

		assertEquals(15_000, cut.getAnnounceInterval(), "Incorrect interval");
		assertEquals("Idle", cut.getStatus(), "Status should have returned to idle");
		verify(torrentClientMock, never()).getPeerConnector();
	}

	@Test
	public void testAnnounceBlockDuplicateAnnounce() throws Exception {
		BencodedMap announceResult = new BencodedMap();
		announceResult.put("interval", new BencodedInteger(15_000));

		announceResult.put("peers", new BencodedList());
		OutStream outStream = new OutStream();
		outStream.write(announceResult.serialize());

		final String url = String.format("http://localhost:%d/announce", wireMock.port());

		WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/announce"))
			// Can't match the exact requests as jetty parses the incorrect UTF-8.
			.withQueryParam("info_hash", WireMock.matching(".*"))
			.withQueryParam("peer_id", WireMock.matching(".*"))
			.withQueryParam("port", WireMock.equalTo("27960"))
			.withQueryParam("uploaded", WireMock.equalTo("0"))
			.withQueryParam("downloaded", WireMock.equalTo("0"))
			.withQueryParam("left", WireMock.equalTo("0"))
			.withQueryParam("compact", WireMock.equalTo("0"))
			.willReturn(WireMock.aResponse().withBody(outStream.toByteArray()))
		);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getPeerId()).thenReturn(peerId);
		when(torrentClientMock.getDownloadPort()).thenReturn(27960);
		when(torrentClientMock.getExecutorService()).thenReturn(new ExecutorServiceMock());

		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getHash()).thenReturn(torrentHash);

		HttpTracker cut = new HttpTracker.Builder()
			.setTorrentClient(torrentClientMock)
			.setUrl(url)
			.build();

		cut.addTorrent(torrentMock);

		assertTrue(cut.hasTorrent(torrentMock), "Add of torrent failed");

		cut.getInfo(torrentMock).get().setEvent(TrackerEvent.EVENT_NONE);
		cut.announce(torrentMock);

		assertEquals(15_000, cut.getAnnounceInterval(), "Incorrect interval");
		assertEquals("Idle", cut.getStatus(), "Status should have returned to idle");
		verify(torrentClientMock, never()).getPeerConnector();

		// This request should be denied because the interval hasn't expired yet.
		cut.announce(torrentMock);

		WireMock.verify(1, WireMock.getRequestedFor(WireMock.urlPathEqualTo("/announce")));
	}

	@Test
	public void testAnnounceFailed() throws Exception {
		BencodedMap announceResult = new BencodedMap();
		announceResult.put("failure reason", new BencodedString("Test failure path."));

		OutStream outStream = new OutStream();
		outStream.write(announceResult.serialize());

		final String url = String.format("http://localhost:%d/announce", wireMock.port());

		WireMock.stubFor(WireMock.get(WireMock.urlPathEqualTo("/announce"))
			// Can't match the exact requests as jetty parses the incorrect UTF-8.
			.withQueryParam("info_hash", WireMock.matching(".*"))
			.withQueryParam("peer_id", WireMock.matching(".*"))
			.withQueryParam("port", WireMock.equalTo("27960"))
			.withQueryParam("uploaded", WireMock.equalTo("0"))
			.withQueryParam("downloaded", WireMock.equalTo("0"))
			.withQueryParam("left", WireMock.equalTo("0"))
			.withQueryParam("compact", WireMock.equalTo("0"))
			.withQueryParam("event", WireMock.equalTo("completed"))
			.willReturn(WireMock.aResponse().withBody(outStream.toByteArray()))
		);

		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getPeerId()).thenReturn(peerId);
		when(torrentClientMock.getDownloadPort()).thenReturn(27960);
		when(torrentClientMock.getExecutorService()).thenReturn(new ExecutorServiceMock());

		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getHash()).thenReturn(torrentHash);

		HttpTracker cut = new HttpTracker.Builder()
			.setTorrentClient(torrentClientMock)
			.setUrl(url)
			.build();

		cut.addTorrent(torrentMock);

		assertTrue(cut.hasTorrent(torrentMock), "Add of torrent failed");

		cut.getInfo(torrentMock).get().setEvent(TrackerEvent.EVENT_COMPLETED);
		cut.announce(torrentMock);

		assertEquals("Announce failed", cut.getStatus(), "Status should not have returned to idle");
		verify(torrentClientMock, never()).getPeerConnector();
	}

	@Test
	public void testComplianceArchLinuxTracker() throws Exception {
		String trackerUrl = "http://tracker.archlinux.org:6969/announce";

		LOGGER.info("Preparing Tracker instance");

		ExecutorServiceMock executorServiceMock = new ExecutorServiceMock();

		byte[] peerId = DummyEntity.createPeerId();
		TorrentClient torrentClientMock = mock(TorrentClient.class);
		when(torrentClientMock.getPeerId()).thenReturn(peerId);
		when(torrentClientMock.getExecutorService()).thenReturn(executorServiceMock);

		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getHash()).thenReturn(torrentHash);

		HttpTracker tracker = new HttpTracker.Builder()
			.setTorrentClient(torrentClientMock)
			.setUrl(trackerUrl)
			.build();

		LOGGER.info("Announcing with start event to tracker...");

		tracker.addTorrent(torrentMock);
		tracker.announce(torrentMock);

		assertEquals("Idle", tracker.getStatus(), "Status should have returned to idle");

		LOGGER.info("Announce has completed.");

		tracker.getInfo(torrentMock).get().setEvent(TrackerEvent.EVENT_STOPPED);
		LOGGER.info("Announcing with stopped event to tracker...");

		tracker.announce(torrentMock);

		assertEquals("Idle", tracker.getStatus(), "Status should have returned to idle");

		LOGGER.info("Announce completed.");
	}


}
