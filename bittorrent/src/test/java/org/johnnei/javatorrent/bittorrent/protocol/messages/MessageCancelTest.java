package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;
import org.johnnei.javatorrent.torrent.peer.PeerDirection;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

		assertEquals("Incorrect message length", 1 + expectedBytes.length, cut.getLength());
		assertArrayEquals("Incorrect output", expectedBytes, outStream.toByteArray());
		assertEquals("Incorrect message ID", 8, cut.getId());
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageCancel["));
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