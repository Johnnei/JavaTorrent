package org.johnnei.javatorrent.torrent.algos.pieceselector;

import java.util.Optional;

import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockSupport;
import org.junit.Test;

import static org.easymock.EasyMock.expect;
import static org.johnnei.javatorrent.test.TestUtils.assertNotPresent;
import static org.johnnei.javatorrent.test.TestUtils.assertPresent;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link MetadataSelect}
 */
public class MetadataSelectTest extends EasyMockSupport {

	@Test
	public void testGetPieceForPeer() throws Exception {
		Torrent torrentMock = createNiceMock(Torrent.class);
		Metadata metadataMock = createMock(Metadata.class);
		MetadataFileSet metadataFileSetMock = createNiceMock(MetadataFileSet.class);
		Peer peerMock = createNiceMock(Peer.class);
		Piece pieceMock = createNiceMock(Piece.class);

		expect(torrentMock.getMetadata()).andReturn(metadataMock);
		expect(metadataMock.getFileSet()).andReturn(Optional.of(metadataFileSetMock));
		expect(metadataFileSetMock.getPiece(0)).andReturn(pieceMock);
		expect(pieceMock.hasBlockWithStatus(BlockStatus.Needed)).andReturn(true);

		replayAll();

		MetadataSelect cut = new MetadataSelect(torrentMock);
		Optional<Piece> result = cut.getPieceForPeer(peerMock);

		verifyAll();

		assertPresent("Piece should be available", result);
		assertEquals("Only one piece is available", pieceMock, result.get());
	}

	@Test
	public void testGetPieceForPeerNothingNeeded() throws Exception {
		Torrent torrentMock = createNiceMock(Torrent.class);
		Metadata metadataMock = createMock(Metadata.class);
		MetadataFileSet metadataFileSetMock = createNiceMock(MetadataFileSet.class);
		Peer peerMock = createNiceMock(Peer.class);
		Piece pieceMock = createNiceMock(Piece.class);

		expect(torrentMock.getMetadata()).andReturn(metadataMock);
		expect(metadataMock.getFileSet()).andReturn(Optional.of(metadataFileSetMock));
		expect(metadataFileSetMock.getPiece(0)).andReturn(pieceMock);
		expect(pieceMock.hasBlockWithStatus(BlockStatus.Needed)).andReturn(false);

		replayAll();

		MetadataSelect cut = new MetadataSelect(torrentMock);
		Optional<Piece> result = cut.getPieceForPeer(peerMock);

		verifyAll();

		assertNotPresent("No piece should be available, none are needed.", result);
	}

	@Test
	public void testGetPieceForPeerNoMetadata() throws Exception {
		Torrent torrentMock = createNiceMock(Torrent.class);
		Metadata metadataMock = createMock(Metadata.class);
		expect(torrentMock.getMetadata()).andReturn(metadataMock);
		expect(metadataMock.getFileSet()).andReturn(Optional.empty());

		Peer peerMock = createNiceMock(Peer.class);

		replayAll();

		MetadataSelect cut = new MetadataSelect(torrentMock);
		Optional<Piece> result = cut.getPieceForPeer(peerMock);

		verifyAll();

		assertNotPresent("No piece should be available, there is no metadata", result);
	}

}