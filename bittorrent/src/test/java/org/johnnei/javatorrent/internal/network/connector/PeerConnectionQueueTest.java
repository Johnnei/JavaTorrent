package org.johnnei.javatorrent.internal.network.connector;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.network.PeerConnectInfo;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.Torrent;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PeerConnectionQueueTest {

	@Test
	@DisplayName("testPoll - Should return null on empty list")
	public void testPollEmpty() {
		PeerConnectionQueue cut = new PeerConnectionQueue();

		assertThat(cut.poll(), is(nullValue()));
	}

	@Test
	@DisplayName("testPoll - Should provide equal priority over all torrents")
	public void testPoll() {
		PeerConnectionQueue cut = new PeerConnectionQueue();

		PeerConnectionState peerOne = createPeer("1", "A");
		PeerConnectionState peerTwo = createPeer("2", "A");
		PeerConnectionState peerThree = createPeer("3", "B");
		PeerConnectionState peerFour = createPeer("3", "C");

		cut.add(peerOne);
		cut.add(peerTwo);
		cut.add(peerThree);
		cut.add(peerFour);

		assertThat(cut.poll(), sameInstance(peerThree));
		assertThat(cut.poll(), sameInstance(peerOne));
		assertThat(cut.poll(), sameInstance(peerFour));
		assertThat(cut.poll(), sameInstance(peerTwo));
	}

	private PeerConnectionState createPeer(String mockName, String torrentHash) {
		PeerConnectionState connectionState = mock(PeerConnectionState.class, mockName);
		PeerConnectInfo connectInfo = mock(PeerConnectInfo.class);
		Torrent torrent = mock(Torrent.class);
		Metadata metadata = mock(Metadata.class);

		when(connectionState.getPeer()).thenReturn(connectInfo);
		when(connectInfo.getTorrent()).thenReturn(torrent);
		when(torrent.getMetadata()).thenReturn(metadata);
		when(metadata.getHashString()).thenReturn(torrentHash);

		return connectionState;
	}

}
