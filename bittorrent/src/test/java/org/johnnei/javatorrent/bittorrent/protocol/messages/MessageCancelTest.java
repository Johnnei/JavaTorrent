package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link MessageRequest}
 */
public class MessageCancelTest {

	@Test
	public void testWrite() {
		byte[] expectedBytes = { 0, 0, 0, 1, 0, 0, 0, 2, 0, 0, 0, 3 };
		OutStream outStream = new OutStream();

		MessageCancel cut = new MessageCancel(1, 2, 3);

		cut.write(outStream);

		assertEquals(1 + expectedBytes.length, cut.getLength(), "Incorrect message length");
		assertArrayEquals(expectedBytes, outStream.toByteArray(), "Incorrect output");
		assertEquals(8, cut.getId(), "Incorrect message ID");
		assertTrue(cut.toString().startsWith("MessageCancel["), "Incorrect toString start.");
	}

	@Test
	public void testReadAndProcess() {
		InStream inStream = new InStream(new byte[] {
				0, 0, 0, 1,
				0, 0, 0, 2,
				0, 0, 0, 3
		});

		Peer peerMock = mock(Peer.class);
		Piece pieceMock = mock(Piece.class);
		Torrent torrentMock = mock(Torrent.class);
		TorrentFileSet torrentFileSet = mock(TorrentFileSet.class);

		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.getFileSet()).thenReturn(torrentFileSet);
		when(torrentFileSet.getPiece(1)).thenReturn(pieceMock);

		MessageCancel cut = new MessageCancel();
		cut.read(inStream);
		cut.process(peerMock);

		verify(peerMock).cancelBlockRequest(pieceMock, 2, 3, PeerDirection.Upload);
	}

}
