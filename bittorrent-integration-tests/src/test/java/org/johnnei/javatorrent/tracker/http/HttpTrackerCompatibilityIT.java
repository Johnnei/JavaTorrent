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
		(byte) 0x1d,(byte) 0x37,(byte) 0x8e,(byte) 0x95,
		(byte) 0xa2,(byte) 0x31,(byte) 0x91,(byte) 0x65,
		(byte) 0xa8,(byte) 0x21,(byte) 0x04,(byte) 0xc2,
		(byte) 0xe9,(byte) 0xf5,(byte) 0xa1,(byte) 0x86,
		(byte) 0x8b,(byte) 0x61,(byte) 0xec,(byte) 0x09
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
