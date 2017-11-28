package org.johnnei.javatorrent.tracker;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.TorrentClient;
import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.torrent.Torrent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PeerConnectorPool}
 */
public class PeerConnectorPoolTest {

	private IPeerConnector peerConnectorMockOne;

	private IPeerConnector peerConnectorMockTwo;


	private PeerConnectorPool cut;

	@BeforeEach
	public void setUp() {
		peerConnectorMockOne = mock(IPeerConnector.class);
		peerConnectorMockTwo = mock(IPeerConnector.class);

		TorrentClient torrentClientMock = mock(TorrentClient.class);

		cut = new PeerConnectorPool(torrentClientMock, 2);
		Whitebox.setInternalState(cut, "connectors", Arrays.asList(peerConnectorMockOne, peerConnectorMockTwo));
	}

	@Test
	public void testStart() throws Exception {
		cut.start();

		verify(peerConnectorMockOne).start();
		verify(peerConnectorMockTwo).start();
	}

	@Test
	public void testStop() throws Exception {
		cut.stop();

		verify(peerConnectorMockOne).stop();
		verify(peerConnectorMockTwo).stop();
	}

	@Test
	public void testEnqueuePeer() throws Exception {
		when(peerConnectorMockOne.getConnectingCount()).thenReturn(3);
		when(peerConnectorMockTwo.getConnectingCount()).thenReturn(2);

		PeerConnectInfo peerConnectInfoMock = mock(PeerConnectInfo.class);

		cut.enqueuePeer(peerConnectInfoMock);

		verify(peerConnectorMockTwo).enqueuePeer(same(peerConnectInfoMock));
	}

	@Test
	public void testGetConnectingCountFor() throws Exception {
		Torrent torrentMock = mock(Torrent.class);
		when(peerConnectorMockOne.getConnectingCountFor(same(torrentMock))).thenReturn(3);
		when(peerConnectorMockTwo.getConnectingCountFor(same(torrentMock))).thenReturn(2);

		int result = cut.getConnectingCountFor(torrentMock);

		assertEquals(5, result, "Incorrect connecting count returned");
	}

	@Test
	public void testGetConnectingCount() throws Exception {
		when(peerConnectorMockOne.getConnectingCount()).thenReturn(10);
		when(peerConnectorMockTwo.getConnectingCount()).thenReturn(5);

		int result = cut.getConnectingCount();

		assertEquals(15, result, "Incorrect sum of connecting peers");
	}
}
