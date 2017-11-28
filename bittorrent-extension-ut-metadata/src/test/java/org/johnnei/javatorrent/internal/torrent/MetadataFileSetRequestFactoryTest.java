package org.johnnei.javatorrent.internal.torrent;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.bittorrent.protocol.messages.IMessage;
import org.johnnei.javatorrent.protocol.extension.PeerExtensions;
import org.johnnei.javatorrent.protocol.messages.extension.MessageExtension;
import org.johnnei.javatorrent.torrent.TorrentException;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MetadataFileSetRequestFactory}
 */
public class MetadataFileSetRequestFactoryTest {

	private Peer peerMock = mock(Peer.class);
	private Piece pieceMock = mock(Piece.class);
	private PeerExtensions peerExtensionsMock = mock(PeerExtensions.class);

	@Test
	public void testCreateRequestFor() throws Exception {
		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.of(peerExtensionsMock));
		when(peerExtensionsMock.getExtensionId("ut_metadata")).thenReturn(1);

		MetadataFileSetRequestFactory cut = new MetadataFileSetRequestFactory();
		IMessage request = cut.createRequestFor(peerMock, pieceMock, 0, 5);

		assertEquals(MessageExtension.class, request.getClass(), "Incorrect message type");
	}

	@Test
	public void testCreateRequestForMissingExtensions() throws Exception {
		when(peerMock.getModuleInfo(PeerExtensions.class)).thenReturn(Optional.empty());

		MetadataFileSetRequestFactory cut = new MetadataFileSetRequestFactory();
		Exception e = assertThrows(TorrentException.class, () -> cut.createRequestFor(peerMock, pieceMock, 0, 5));
		assertThat(e.getMessage(), containsString("ut_metadata"));
	}

	@Test
	public void testCreateCancelRequestFor() throws Exception {
		Exception e = assertThrows(UnsupportedOperationException.class, () -> new MetadataFileSetRequestFactory().createCancelRequestFor(null, null, 0, 0));
		assertThat(e.getMessage(), containsString("cancel"));
	}

	@Test
	public void testSupportsCancellation() throws Exception {
		assertFalse(new MetadataFileSetRequestFactory().supportsCancellation(), "ut_metadata doesn't define a way to cancel requests.");
	}

}
