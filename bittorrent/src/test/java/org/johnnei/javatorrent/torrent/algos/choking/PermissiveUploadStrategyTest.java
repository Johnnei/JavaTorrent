package org.johnnei.javatorrent.torrent.algos.choking;

import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.junit.Test;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link PermissiveUploadStrategy}
 */
public class PermissiveUploadStrategyTest {

	@Test
	public void testUpdateChokingDontSwitchState() throws Exception {
		Peer peerMock = mock(Peer.class);

		when(peerMock.isInterested(PeerDirection.Upload)).thenReturn(false);
		when(peerMock.isChoked(PeerDirection.Upload)).thenReturn(true);

		PermissiveUploadStrategy cut = new PermissiveUploadStrategy();
		cut.updateChoking(peerMock);

		verify(peerMock, never()).setChoked(eq(PeerDirection.Upload), anyBoolean());
	}

	@Test
	public void testUpdateChokingSwitchStateToFalse() throws Exception {
		Peer peerMock = mock(Peer.class);

		when(peerMock.isInterested(PeerDirection.Upload)).thenReturn(true);
		when(peerMock.isChoked(PeerDirection.Upload)).thenReturn(true);

		PermissiveUploadStrategy cut = new PermissiveUploadStrategy();
		cut.updateChoking(peerMock);

		verify(peerMock).setChoked(PeerDirection.Upload, false);
	}

	@Test
	public void testUpdateChokingSwitchStateToTrue() throws Exception {
		Peer peerMock = mock(Peer.class);

		when(peerMock.isInterested(PeerDirection.Upload)).thenReturn(false);
		when(peerMock.isChoked(PeerDirection.Upload)).thenReturn(false);

		PermissiveUploadStrategy cut = new PermissiveUploadStrategy();
		cut.updateChoking(peerMock);

		verify(peerMock).setChoked(PeerDirection.Upload, true);
	}

}