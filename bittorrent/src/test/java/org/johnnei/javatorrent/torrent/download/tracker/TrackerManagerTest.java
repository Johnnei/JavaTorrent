package org.johnnei.javatorrent.torrent.download.tracker;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.same;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.johnnei.javatorrent.torrent.download.Torrent;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class TrackerManagerTest extends EasyMockSupport {

	private TrackerManager cut;

	@Mock
	private IPeerConnector peerConnectorMock;

	@Mock
	private TrackerFactory trackerFactoryMock;

	@Before
	public void setUp() {
		cut = new TrackerManager(peerConnectorMock, trackerFactoryMock);
	}

	@Test
	public void testAnnounce() {
		Torrent torrentMock = createMock(Torrent.class);

		List<ITracker> trackers = Arrays.asList(createMock(ITracker.class), createMock(ITracker.class));
		trackers.forEach(trackerMock -> {
			trackerMock.announce(same(torrentMock));
			expectLastCall();
		});

		expect(trackerFactoryMock.getTrackingsHavingTorrent(same(torrentMock))).andReturn(trackers);
		replayAll();

		cut.announce(torrentMock);

		verifyAll();
	}

	@Test
	public void testGetConnectingCountFor() {
		Torrent torrentMock = createMock(Torrent.class);
		final int connectingCount = 5;

		expect(peerConnectorMock.getConnectingCountFor(same(torrentMock))).andReturn(connectingCount);
		replayAll();

		int connecting = cut.getConnectingCountFor(torrentMock);

		Assert.assertEquals("Incorrect connecting amount", connectingCount, connecting);
	}

	@Test
	public void testConstructor() {
		byte[] peerId = cut.getPeerId();

		// Assert that the peer id is in format: -JTdddd-xxxxxxxxxxxx
		// The first 8 bytes are always readable ASCII characters.
		String clientIdentifier = new String(peerId, 0, 8);
		Assert.assertTrue("Incorrect client identifier in peer ID", Pattern.matches("-JT\\d{4}-", clientIdentifier));
	}

	@Test
	public void testAddTorrent() {
		Torrent torrent = createMock(Torrent.class);
		ITracker trackerMock = createMock(ITracker.class);
		final String trackerUrl = "udp://localhost:80";

		expect(trackerFactoryMock.getTrackerFor(eq(trackerUrl))).andReturn(trackerMock);
		trackerMock.addTorrent(same(torrent));
		expectLastCall();

		replayAll();
		cut.addTorrent(torrent, trackerUrl);

		verifyAll();
	}

	@Test
	public void testCreateUniqueTransactionId() {
		int id = cut.createUniqueTransactionId();
		int id2 = cut.createUniqueTransactionId();

		Assert.assertNotEquals("Duplicate transaction IDs", id, id2);
	}

}
