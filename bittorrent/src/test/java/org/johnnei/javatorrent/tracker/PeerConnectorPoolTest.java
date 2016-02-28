package org.johnnei.javatorrent.tracker;

import java.util.Arrays;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.test.Whitebox;
import org.johnnei.javatorrent.torrent.Torrent;

import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link PeerConnectorPool}
 */
public class PeerConnectorPoolTest extends EasyMockSupport {

	private IPeerConnector peerConnectorMockOne;

	private IPeerConnector peerConnectorMockTwo;


	private PeerConnectorPool cut;

	@Before
	public void setUp() {
		peerConnectorMockOne = createMock(IPeerConnector.class);
		peerConnectorMockTwo = createMock(IPeerConnector.class);

		// Because the torrent client is irrelevant to the test this mock is being managed within the setup method.
		TorrentClient torrentClientMock = EasyMock.createMock(TorrentClient.class);

		replay(torrentClientMock);
		cut = new PeerConnectorPool(torrentClientMock, 2);
		verify(torrentClientMock);
		Whitebox.setInternalState(cut, "connectors", Arrays.asList(peerConnectorMockOne, peerConnectorMockTwo));
	}

	@Test
	public void testStart() throws Exception {
		peerConnectorMockOne.start();
		peerConnectorMockTwo.start();
		replayAll();

		cut.start();

		verifyAll();
	}

	@Test
	public void testStop() throws Exception {
		peerConnectorMockOne.stop();
		peerConnectorMockTwo.stop();
		replayAll();

		cut.stop();

		verifyAll();
	}

	@Test
	public void testEnqueuePeer() throws Exception {
		expect(peerConnectorMockOne.getConnectingCount()).andStubReturn(3);
		expect(peerConnectorMockTwo.getConnectingCount()).andStubReturn(2);

		PeerConnectInfo peerConnectInfoMock = createMock(PeerConnectInfo.class);

		peerConnectorMockTwo.enqueuePeer(same(peerConnectInfoMock));

		replayAll();

		cut.enqueuePeer(peerConnectInfoMock);

		verifyAll();
	}

	@Test
	public void testGetConnectingCountFor() throws Exception {
		Torrent torrentMock = createMock(Torrent.class);
		expect(peerConnectorMockOne.getConnectingCountFor(same(torrentMock))).andReturn(3);
		expect(peerConnectorMockTwo.getConnectingCountFor(same(torrentMock))).andReturn(2);

		replayAll();

		int result = cut.getConnectingCountFor(torrentMock);

		verifyAll();

		assertEquals("Incorrect connecting count returned", 5, result);
	}

	@Test
	public void testGetConnectingCount() throws Exception {
		expect(peerConnectorMockOne.getConnectingCount()).andReturn(10);
		expect(peerConnectorMockTwo.getConnectingCount()).andReturn(5);
		replayAll();

		int result = cut.getConnectingCount();

		verifyAll();

		assertEquals("Incorrect sum of connecting peers", 15, result);
	}
}