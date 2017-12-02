package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.network.BitTorrentSocket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MessageRequest}
 */
public class MessageRequestTest {

	@Test
	public void testWrite() {
		byte[] expectedBytes = { 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3 };
		OutStream outStream = new OutStream();

		MessageRequest cut = new MessageRequest(1, 2, 3);

		cut.write(outStream);

		assertEquals(1 + expectedBytes.length, cut.getLength(), "Incorrect message length");
		assertArrayEquals(expectedBytes, outStream.toByteArray(), "Incorrect output");
		assertEquals(6, cut.getId(), "Incorrect message ID");
		assertTrue(cut.toString().startsWith("MessageRequest["), "Incorrect toString start.");
	}

	@Test
	public void testReadAndProcess() {
		InStream inStream = new InStream(new byte[]{
			0, 0, 0, 1,
			0, 0, 0, 2,
			0, 0, 0, 3
		});

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		Piece pieceMock = mock(Piece.class);

		when(filesMock.getPiece(1)).thenReturn(pieceMock);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.getFileSet()).thenReturn(filesMock);
		when(filesMock.hasPiece(eq(1))).thenReturn(true);

		MessageRequest cut = new MessageRequest();
		cut.read(inStream);
		cut.process(peerMock);

		verify(peerMock).addBlockRequest(eq(pieceMock), eq(2), eq(3), eq(PeerDirection.Upload));
	}

	@Test
	public void testReadAndProcessNotHavingPiece() {
		InStream inStream = new InStream(new byte[]{
			0, 0, 0, 1,
			0, 0, 0, 2,
			0, 0, 0, 3
		});

		Peer peerMock = mock(Peer.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet filesMock = mock(TorrentFileSet.class);
		BitTorrentSocket socketMock = mock(BitTorrentSocket.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.getFileSet()).thenReturn(filesMock);
		when(filesMock.hasPiece(eq(1))).thenReturn(false);
		when(peerMock.getBitTorrentSocket()).thenReturn(socketMock);

		MessageRequest cut = new MessageRequest();
		cut.read(inStream);
		cut.process(peerMock);

		verify(socketMock).close();
	}
}
