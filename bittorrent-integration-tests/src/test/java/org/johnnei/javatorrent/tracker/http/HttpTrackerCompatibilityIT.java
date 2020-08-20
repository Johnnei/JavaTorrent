package org.johnnei.javatorrent.tracker.http;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.internal.tracker.http.HttpTracker;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.ExecutorServiceMock;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link HttpTracker}
 */
public class HttpTrackerCompatibilityIT {

	public static final Logger LOGGER = LoggerFactory.getLogger(HttpTrackerCompatibilityIT.class);

	// @formatter:off
	public static final byte[] TORRENT_HASH = new byte[] {
		(byte) 0x49, (byte) 0xda, (byte) 0x7a, (byte) 0xe0, (byte) 0xde,
		(byte) 0x88, (byte) 0x74, (byte) 0x46, (byte) 0x24, (byte) 0x71,
		(byte) 0xd0, (byte) 0xf5, (byte) 0x41, (byte) 0x9b, (byte) 0x85,
		(byte) 0x0e, (byte) 0x59, (byte) 0x9b, (byte) 0x05, (byte) 0xef
	};

	public static final byte[] PEER_ID = new byte [] {
		       0x58, 0x58,        0x58,        0x58,        0x58,
		       0x58, 0x58,        0x58,        0x58,        0x58,
		       0x58, 0x58,        0x58,        0x58, (byte) 0x58,
		(byte) 0x58, 0x58, (byte) 0x58, (byte) 0x58,        0x58
	};
	// @formatter:on

	@Test
	@Disabled("ArchLinux Tracker has issues with their ISO image torrent")
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
		when(metadataMock.getHash()).thenReturn(TORRENT_HASH);

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
