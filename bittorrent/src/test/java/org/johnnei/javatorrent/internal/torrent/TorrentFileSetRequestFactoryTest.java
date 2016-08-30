package org.johnnei.javatorrent.internal.torrent;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageCancel;
import org.johnnei.javatorrent.bittorrent.protocol.messages.MessageRequest;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.junit.Test;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests {@link TorrentFileSetRequestFactory}
 */
public class TorrentFileSetRequestFactoryTest {

	@Test
	public void testCreateRequestFor() throws Exception {
		Peer peerMock = mock(Peer.class);
		Piece pieceMock = mock(Piece.class);

		TorrentFileSetRequestFactory cut = new TorrentFileSetRequestFactory();
		IMessage result = cut.createRequestFor(peerMock, pieceMock, 42, 7);
		assertThat("Incorrect message type", result, instanceOf(MessageRequest.class));
	}

	@Test
	public void testCreateCancelRequestFor() throws Exception {
		Piece pieceMock = mock(Piece.class);

		TorrentFileSetRequestFactory cut = new TorrentFileSetRequestFactory();
		IMessage result = cut.createCancelRequestFor(pieceMock, 42, 7);
		assertThat("Incorrect message type", result, instanceOf(MessageCancel.class));
	}

	@Test
	public void testSupportsCancellation() throws Exception {
		TorrentFileSetRequestFactory cut = new TorrentFileSetRequestFactory();
		assertThat("BitTorrent spec defines a message to cancel a block request.", cut.supportsCancellation(), is(true));
	}

}