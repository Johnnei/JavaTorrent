package org.johnnei.javatorrent.internal.tracker.udp;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;

import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.bittorrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerAction;
import org.johnnei.javatorrent.bittorrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.tracker.UdpTracker;

import static org.johnnei.javatorrent.test.TestUtils.copySection;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AnnounceRequestTest {

	@Test
	public void testAnnounceRequest() {
		TorrentInfo info = new TorrentInfo(DummyEntity.createUniqueTorrent(), Clock.systemDefaultZone());
		assertThrows(IllegalArgumentException.class, () -> new AnnounceRequest(info, new byte[0], 5));
	}

	@Test
	public void testWriteRequestNoEventAndPreData() {
		final byte[] DOWNLOADED_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		final byte[] UPLOADED_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		final byte[] REMAINING_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		final byte[] EVENT_BYTES = new byte[] { 0, 0, 0, 0 };
		final byte[] SENDER_IP_BYTES = new byte[] { 0, 0, 0, 0 };
		final byte[] PEERS_WANTED_BYES = new byte[] { 0, 0, 0, 0x32 };
		final byte[] PORT_BYTES = new byte[] { (byte) 0x6D, 0x38 };
		final byte[] EXTENSION_BYTES = new byte[] { 0, 0 };

		OutStream outStream = new OutStream();

		Torrent torrent = new Torrent.Builder()
				.setMetadata(new Metadata.Builder().setHash(DummyEntity.createRandomBytes(20)).build())
				.setName("Dummy Torrent")
				.build();

		TorrentInfo info = new TorrentInfo(torrent, Clock.systemDefaultZone());
		info.setEvent(TrackerEvent.EVENT_NONE);
		byte[] peerId = DummyEntity.createPeerId();
		AnnounceRequest request = new AnnounceRequest(info, peerId, 27960);

		request.writeRequest(outStream);
		byte[] actual = outStream.toByteArray();

		byte[] expectedOutput = new byte[84];
		copySection(torrent.getMetadata().getHash(), expectedOutput, 0);
		copySection(peerId, expectedOutput, 20);
		copySection(DOWNLOADED_BYTES, expectedOutput, 40);
		copySection(REMAINING_BYTES, expectedOutput, 48);
		copySection(UPLOADED_BYTES, expectedOutput, 56);
		copySection(EVENT_BYTES, expectedOutput, 64);
		copySection(SENDER_IP_BYTES, expectedOutput, 68);
		// We can't guess the key field, copy it from the source
		copySection(actual, expectedOutput, 72, 72, 4);
		copySection(PEERS_WANTED_BYES, expectedOutput, 76);
		copySection(PORT_BYTES, expectedOutput, 80);
		copySection(EXTENSION_BYTES, expectedOutput, 82);

		assertEquals(84, actual.length, "Written bytes has incorrect length");
		assertArrayEquals(expectedOutput, actual, "Written bytes output is incorrect.");
	}

	@Test
	public void testWriteRequestStartedEvent() {
		final byte[] DOWNLOADED_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 5 };
		final byte[] UPLOADED_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 7 };
		final byte[] REMAINING_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 3 };
		final byte[] EVENT_BYTES = new byte[] { 0, 0, 0, 2 };
		final byte[] SENDER_IP_BYTES = new byte[] { 0, 0, 0, 0 };
		final byte[] PEERS_WANTED_BYES = new byte[] { 0, 0, 0, 0x32 };
		final byte[] PORT_BYTES = new byte[] { (byte) 0x6D, 0x38 };
		final byte[] EXTENSION_BYTES = new byte[] { 0, 0 };

		OutStream outStream = new OutStream();

		TorrentFileSet torrentFileSetMock = mock(TorrentFileSet.class);
		when(torrentFileSetMock.countRemainingBytes()).thenReturn(3L);

		Torrent torrent = new Torrent.Builder()
				.setMetadata(new Metadata.Builder().setHash(DummyEntity.createUniqueTorrentHash()).build())
				.setName("Dummy Torrent")
				.build();
		torrent.setFileSet(torrentFileSetMock);

		Whitebox.setInternalState(torrent, "downloadedBytes", 5);
		torrent.addUploadedBytes(7);

		TorrentInfo info = new TorrentInfo(torrent, Clock.systemDefaultZone());
		byte[] peerId = DummyEntity.createPeerId();
		AnnounceRequest request = new AnnounceRequest(info, peerId, 27960);
		info.setEvent(TrackerEvent.EVENT_STARTED);

		request.writeRequest(outStream);
		byte[] actual = outStream.toByteArray();

		byte[] expectedOutput = new byte[84];
		copySection(torrent.getMetadata().getHash(), expectedOutput, 0);
		copySection(peerId, expectedOutput, 20);
		copySection(DOWNLOADED_BYTES, expectedOutput, 40);
		copySection(REMAINING_BYTES, expectedOutput, 48);
		copySection(UPLOADED_BYTES, expectedOutput, 56);
		copySection(EVENT_BYTES, expectedOutput, 64);
		copySection(SENDER_IP_BYTES, expectedOutput, 68);
		// We can't guess the key field, copy it from the source
		copySection(actual, expectedOutput, 72, 72, 4);
		copySection(PEERS_WANTED_BYES, expectedOutput, 76);
		copySection(PORT_BYTES, expectedOutput, 80);
		copySection(EXTENSION_BYTES, expectedOutput, 82);

		assertEquals(84, actual.length, "Written bytes has incorrect length");
		assertArrayEquals(expectedOutput, actual, "Written bytes output is incorrect.");
	}

	@Test
	public void testReadResponse() throws Exception {
		byte[] inputData = new byte[] {
				// Interval: 30 seconds
				0x00, 0x00, 0x75, 0x30,
				// 5 leechers
				0x00, 0x00, 0x00, 0x05,
				// 42 seeders
				0x00, 0x00, 0x00, 0x2A,
				// Peer: 0.0.0.0:0
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				// Peer: 127.0.0.1:27960
				0x7F, 0x00, 0x00, 0x01, (byte) 0x6D, 0x38
		};

		InStream inStream = new InStream(inputData);
		TorrentInfo info = new TorrentInfo(DummyEntity.createUniqueTorrent(), Clock.systemDefaultZone());
		AnnounceRequest request = new AnnounceRequest(info, DummyEntity.createPeerId(), 27960);

		request.readResponse(inStream);

		UdpTracker trackerMock = mock(UdpTracker.class);
		PeerConnectInfo peerInfo = new PeerConnectInfo(info.getTorrent(), new InetSocketAddress(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), 27960));

		request.process(trackerMock);

		verify(trackerMock).connectPeer(eq(peerInfo));
		verify(trackerMock).setAnnounceInterval(eq(30_000));

		assertEquals(5, info.getLeechers(), "Incorrect leechers amount");
		assertEquals(42, info.getSeeders(), "Incorrect seeders amount");
	}

	@Test
	public void testReadBrokenResponse() throws Exception {
		byte[] inputData = new byte[] {
				// Interval: 30 seconds
				0x00, 0x00, 0x75, 0x30,
				// 5 leechers
				0x00, 0x00, 0x00, 0x05,
				// 42 seeders
				0x00, 0x00, 0x00, 0x2A,
				// Peer: 0.0.0.0:0
				0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
				// Peer: 127.0.0.1:27960
				0x7F, 0x00, 0x00, 0x01, (byte) 0x6D
		};

		InStream inStream = new InStream(inputData);
		TorrentInfo info = new TorrentInfo(DummyEntity.createUniqueTorrent(), Clock.systemDefaultZone());
		AnnounceRequest request = new AnnounceRequest(info, DummyEntity.createPeerId(), 27960);

		request.readResponse(inStream);

		UdpTracker trackerMock = mock(UdpTracker.class);

		request.process(trackerMock);

		verify(trackerMock).setAnnounceInterval(eq(30_000));

		assertEquals(5, info.getLeechers(), "Incorrect leechers amount");
		assertEquals(42, info.getSeeders(), "Incorrect seeders amount");
	}

	@Test
	public void testGetAction() {
		AnnounceRequest request = new AnnounceRequest(
				new TorrentInfo(DummyEntity.createUniqueTorrent(), Clock.systemDefaultZone()),
				DummyEntity.createPeerId(),
				5);

		assertEquals(TrackerAction.ANNOUNCE, request.getAction(), "Incorrect action.");
	}

	@Test
	public void testGetMinimalSize() {
		AnnounceRequest request = new AnnounceRequest(
				new TorrentInfo(DummyEntity.createUniqueTorrent(), Clock.systemDefaultZone()),
				DummyEntity.createPeerId(),
				5);

		assertEquals(12, request.getMinimalSize(), "Incorrect minimal size.");
	}

}
