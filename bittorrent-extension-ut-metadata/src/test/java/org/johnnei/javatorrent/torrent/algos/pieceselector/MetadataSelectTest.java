package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MetadataSelect}
 */
public class MetadataSelectTest {

	@Test
	public void testGetPieceForPeer() throws Exception {
		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		MetadataFileSet metadataFileSetMock = mock(MetadataFileSet.class);
		Peer peerMock = mock(Peer.class);
		Piece pieceMock = mock(Piece.class);

		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getFileSet()).thenReturn(Optional.of(metadataFileSetMock));
		when(metadataFileSetMock.getPiece(0)).thenReturn(pieceMock);
		when(pieceMock.hasBlockWithStatus(BlockStatus.Needed)).thenReturn(true);

		MetadataSelect cut = new MetadataSelect(torrentMock);
		Optional<Piece> result = cut.getPieceForPeer(peerMock);

		assertEquals(pieceMock, assertPresent("Piece should be available", result), "Only one piece is available");
	}

	@Test
	public void testGetPieceForPeerNothingNeeded() throws Exception {
		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		MetadataFileSet metadataFileSetMock = mock(MetadataFileSet.class);
		Peer peerMock = mock(Peer.class);
		Piece pieceMock = mock(Piece.class);

		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getFileSet()).thenReturn(Optional.of(metadataFileSetMock));
		when(metadataFileSetMock.getPiece(0)).thenReturn(pieceMock);
		when(pieceMock.hasBlockWithStatus(BlockStatus.Needed)).thenReturn(false);

		MetadataSelect cut = new MetadataSelect(torrentMock);
		Optional<Piece> result = cut.getPieceForPeer(peerMock);

		assertNotPresent("No piece should be available, none are needed.", result);
	}

	@Test
	public void testGetPieceForPeerNoMetadata() throws Exception {
		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getFileSet()).thenReturn(Optional.empty());

		Peer peerMock = mock(Peer.class);

		MetadataSelect cut = new MetadataSelect(torrentMock);
		Optional<Piece> result = cut.getPieceForPeer(peerMock);

		assertNotPresent("No piece should be available, there is no metadata", result);
	}

}
