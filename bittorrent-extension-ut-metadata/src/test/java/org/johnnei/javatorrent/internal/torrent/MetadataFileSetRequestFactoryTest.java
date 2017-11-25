package org.johnnei.javatorrent.internal.torrent;

import java.util.Optional;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MetadataFileSetRequestFactory}
 */
public class MetadataFileSetRequestFactoryTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private Peer peerMock = mock(Peer.class);
	private Piece pieceMock = mock(Piece.class);
	private PeerExtensions peerExtensionsMock = mock(PeerExtensions.class);

	@Test
	public void testCreateRequestFor() throws Exception {
		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(peerExtensionsMock));
		when(peerExtensionsMock.getExtensionId("ut_metadata")).thenReturn(1);

		MetadataFileSetRequestFactory cut = new MetadataFileSetRequestFactory();
		IMessage request = cut.createRequestFor(peerMock, pieceMock, 0, 5);

		assertEquals("Incorrect message type", MessageExtension.class, request.getClass());
	}

	@Test
	public void testCreateRequestForMissingExtensions() throws Exception {
		thrown.expect(TorrentException.class);
		thrown.expectMessage("ut_metadata");

		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.empty());

		MetadataFileSetRequestFactory cut = new MetadataFileSetRequestFactory();
		cut.createRequestFor(peerMock, pieceMock, 0, 5);
	}

	@Test
	public void testCreateCancelRequestFor() throws Exception {
		thrown.expect(UnsupportedOperationException.class);
		thrown.expectMessage("cancel");

		new MetadataFileSetRequestFactory().createCancelRequestFor(null, null, 0, 0);
	}

	@Test
	public void testSupportsCancellation() throws Exception {
		assertFalse("ut_metadata doesn't define a way to cancel requests.", new MetadataFileSetRequestFactory().supportsCancellation());
	}

}