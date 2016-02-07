package org.johnnei.javatorrent.internal.tracker.udp;

import static org.easymock.EasyMock.eq;
import static org.johnnei.javatorrent.test.TestUtils.copySection;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.johnnei.javatorrent.internal.tracker.udp.AnnounceRequest;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.test.DummyEntity;
import org.johnnei.javatorrent.test.StubEntity;
import org.johnnei.javatorrent.test.Whitebox;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.johnnei.javatorrent.torrent.download.peer.PeerConnectInfo;
import org.johnnei.javatorrent.torrent.tracker.TorrentInfo;
import org.johnnei.javatorrent.torrent.tracker.TrackerAction;
import org.johnnei.javatorrent.torrent.tracker.TrackerEvent;
import org.johnnei.javatorrent.tracker.UdpTracker;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class AnnounceRequestTest extends EasyMockSupport {

	@Test(expected=IllegalArgumentException.class)
	public void testAnnounceRequest() {
		TorrentInfo info = new TorrentInfo(DummyEntity.createTorrent(), Clock.systemDefaultZone());
		new AnnounceRequest(info, new byte[0], 5);
	}

	@Test
	public void testWriteRequestNoEventAndPreData() {
		final byte[] DOWNLOADED_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		final byte[] UPLOADED_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		final byte[] REMAINING_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		final byte[] EVENT_BYTES = new byte[] { 0, 0, 0, 0 };
		final byte[] SENDER_IP_BYTES = new byte[] { 0, 0, 0, 0 };
		final byte[] PEERS_WANTED_BYES = new byte[] { 0, 0, 0, 3 };
		final byte[] PORT_BYTES = new byte[] { (byte) 0x6D, 0x38 };
		final byte[] EXTENSION_BYTES = new byte[] { 0, 0 };

		OutStream outStream = new OutStream();

		Torrent torrent = new Torrent.Builder()
				.setHash(DummyEntity.createRandomBytes(20))
				.setName("Dummy Torrent")
				.setPeerManager(StubEntity.stubPeerManager())
				.build();

		TorrentInfo info = new TorrentInfo(torrent, Clock.systemDefaultZone());
		info.setEvent(TrackerEvent.EVENT_NONE);
		byte[] peerId = DummyEntity.createPeerId();
		AnnounceRequest request = new AnnounceRequest(info, peerId, 27960);

		request.writeRequest(outStream);
		byte[] actual = outStream.toByteArray();

		byte[] expectedOutput = new byte[84];
		copySection(torrent.getHashArray(), expectedOutput, 0);
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

		assertEquals("Written bytes has incorrect length", 84, actual.length);
		assertArrayEquals("Written bytes output is incorrect.", expectedOutput, actual);
	}

	@Test
	public void testWriteRequestStartedEvent() {
		final byte[] DOWNLOADED_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 5 };
		final byte[] UPLOADED_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 7 };
		final byte[] REMAINING_BYTES = new byte[] { 0, 0, 0, 0, 0, 0, 0, 3 };
		final byte[] EVENT_BYTES = new byte[] { 0, 0, 0, 2 };
		final byte[] SENDER_IP_BYTES = new byte[] { 0, 0, 0, 0 };
		final byte[] PEERS_WANTED_BYES = new byte[] { 0, 0, 0, 3 };
		final byte[] PORT_BYTES = new byte[] { (byte) 0x6D, 0x38 };
		final byte[] EXTENSION_BYTES = new byte[] { 0, 0 };

		OutStream outStream = new OutStream();

		Torrent torrent = new Torrent.Builder()
				.setHash(DummyEntity.createRandomBytes(20))
				.setName("Dummy Torrent")
				.setPeerManager(StubEntity.stubPeerManager())
				.build();
		torrent.setFiles(StubEntity.stubAFiles(3));

		Whitebox.setInternalState(torrent, "downloadedBytes", 5);
		torrent.addUploadedBytes(7);

		TorrentInfo info = new TorrentInfo(torrent, Clock.systemDefaultZone());
		byte[] peerId = DummyEntity.createPeerId();
		AnnounceRequest request = new AnnounceRequest(info, peerId, 27960);
		info.setEvent(TrackerEvent.EVENT_STARTED);

		request.writeRequest(outStream);
		byte[] actual = outStream.toByteArray();

		byte[] expectedOutput = new byte[84];
		copySection(torrent.getHashArray(), expectedOutput, 0);
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

		assertEquals("Written bytes has incorrect length", 84, actual.length);
		assertArrayEquals("Written bytes output is incorrect.", expectedOutput, actual);
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
		TorrentInfo info = new TorrentInfo(DummyEntity.createTorrent(), Clock.systemDefaultZone());
		AnnounceRequest request = new AnnounceRequest(info, DummyEntity.createPeerId(), 27960);

		request.readResponse(inStream);

		UdpTracker trackerMock = createMock(UdpTracker.class);
		PeerConnectInfo peerInfo = new PeerConnectInfo(info.getTorrent(), new InetSocketAddress(InetAddress.getByAddress(new byte[] { 127, 0, 0, 1 }), 27960));
		trackerMock.connectPeer(eq(peerInfo));
		trackerMock.setAnnounceInterval(eq(30_000));

		replayAll();

		request.process(trackerMock);

		verifyAll();

		assertEquals("Incorrect leechers amount", 5, info.getLeechers());
		assertEquals("Incorrect seeders amount", 42, info.getSeeders());
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
		TorrentInfo info = new TorrentInfo(DummyEntity.createTorrent(), Clock.systemDefaultZone());
		AnnounceRequest request = new AnnounceRequest(info, DummyEntity.createPeerId(), 27960);

		request.readResponse(inStream);

		UdpTracker trackerMock = createMock(UdpTracker.class);
		trackerMock.setAnnounceInterval(eq(30_000));

		replayAll();

		request.process(trackerMock);

		verifyAll();

		assertEquals("Incorrect leechers amount", 5, info.getLeechers());
		assertEquals("Incorrect seeders amount", 42, info.getSeeders());
	}

	@Test
	public void testGetAction() {
		AnnounceRequest request = new AnnounceRequest(
				new TorrentInfo(DummyEntity.createTorrent(), Clock.systemDefaultZone()),
				DummyEntity.createPeerId(),
				5);

		assertEquals("Incorrect action.", TrackerAction.ANNOUNCE, request.getAction());
	}

	@Test
	public void testGetMinimalSize() {
		AnnounceRequest request = new AnnounceRequest(
				new TorrentInfo(DummyEntity.createTorrent(), Clock.systemDefaultZone()),
				DummyEntity.createPeerId(),
				5);

		assertEquals("Incorrect minimal size.", 12, request.getMinimalSize());
	}

}
