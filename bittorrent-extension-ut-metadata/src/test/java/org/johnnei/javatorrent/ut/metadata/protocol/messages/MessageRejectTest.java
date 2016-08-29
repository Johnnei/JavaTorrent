package org.johnnei.javatorrent.ut.metadata.protocol.messages;

import java.nio.charset.Charset;
import java.util.Optional;

import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.Metadata;
import org.johnnei.javatorrent.torrent.MetadataFileSet;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.files.BlockStatus;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MessageReject}
 */
public class MessageRejectTest {

	@Test
	public void testWrite() {
		MessageReject cut = new MessageReject(5);

		OutStream outStream = new OutStream();
		cut.write(outStream);

		byte[] result = outStream.toByteArray();
		String expectedOutput = "d5:piecei5e8:msg_typei2ee";

		assertEquals("Incorrect length", expectedOutput.getBytes(Charset.forName("UTF-8")).length, cut.getLength());
		assertEquals("Incorrect output", expectedOutput, new String(result, Charset.forName("UTF-8")));
	}

	@Test
	public void testReadAndProcess() {
		InStream inStream = new InStream("d8:msg_typei2e5:piecei5ee".getBytes(Charset.forName("UTF-8")));

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);
		Metadata metadataMock = mock(Metadata.class);
		MetadataFileSet metadataFileSetMock = mock(MetadataFileSet.class);
		Piece pieceMock = mock(Piece.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getFileSet()).thenReturn(Optional.of(metadataFileSetMock));
		when(metadataFileSetMock.getPiece(0)).thenReturn(pieceMock);

		MessageReject cut = new MessageReject();
		cut.read(inStream);
		cut.process(peerMock);

		verify(pieceMock).setBlockStatus(5, BlockStatus.Needed);
		verify(peerMock).onReceivedBlock(pieceMock, 5);
	}

	@Test
	public void testReadAndProcessNoMetadata() {
		InStream inStream = new InStream("d8:msg_typei2e5:piecei5ee".getBytes(Charset.forName("UTF-8")));

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);
		Metadata metadataMock = mock(Metadata.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);
		when(torrentMock.getMetadata()).thenReturn(metadataMock);
		when(metadataMock.getFileSet()).thenReturn(Optional.empty());

		MessageReject cut = new MessageReject();
		cut.read(inStream);
		cut.process(peerMock);

		verify(socketMock).close();
	}

	@Test
	public void testToString() {
		MessageReject cut = new MessageReject();
		assertTrue("Incorrect toString start", cut.toString().startsWith("MessageReject["));
	}

}