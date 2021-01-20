package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.algos.requests.IRequestLimiter;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test {@link MessageBlock}
 */
public class MessageBlockTest {

	private Peer peerMock = mock(Peer.class);
	private Torrent torrentMock = mock(Torrent.class);
	private TorrentFileSet fileSetMock = mock(TorrentFileSet.class);
	private Piece pieceMock = mock(Piece.class);
	private IRequestLimiter requestLimiterMock = mock(IRequestLimiter.class);

	@Test
	public void testStaticMethods() {
		MessageBlock cut = new MessageBlock();

		assertEquals(7 ,cut.getId(), "Incorrect message ID");
		assertTrue(cut.toString().startsWith("MessageBlock["), "Incorrect toString start.");

		cut = new MessageBlock(1, 2, new byte[] {});
		assertTrue(cut.toString().startsWith("MessageBlock["), "Incorrect toString start.");
	}

	private void prepareTest() {
		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(torrentMock.getRequestLimiter()).thenReturn(requestLimiterMock);
		when(pieceMock.getIndex()).thenReturn(5);
		when(fileSetMock.getPiece(5)).thenReturn(pieceMock);
	}

	@Test
	public void testProcessInvalidLength() {
		prepareTest();

		MessageBlock cut = new MessageBlock(5, 0x37FF, new byte[] {});
		cut.process(peerMock);

		verify(peerMock).addStrike(1);
	}

	@Test
	public void testReadAndProcess() {
		InStream inStream = new InStream(new byte[] {
				0x00, 0x00, 0x00, 0x05,
				0x00, 0x00, 0x38, 0x00,
				0x00
		});

		prepareTest();

		when(peerMock.getRequestLimit()).thenReturn(2);

		MessageBlock cut = new MessageBlock();
		cut.read(inStream);
		cut.process(peerMock);

		verify(torrentMock).onReceivedBlock(fileSetMock, 5, 0x3800, new byte[] { 0x00 });
		verify(peerMock).addStrike(-1);
		verify(requestLimiterMock).onReceivedBlock(peerMock, cut);
	}

	@Test
	public void testReadAndProcessHighTrust() {
		InStream inStream = new InStream(new byte[] {
				0x00, 0x00, 0x00, 0x05,
				0x00, 0x00, 0x38, 0x00,
				0x00
		});

		prepareTest();

		when(peerMock.getRequestLimit()).thenReturn(5);

		MessageBlock cut = new MessageBlock();
		cut.read(inStream);
		cut.process(peerMock);

		verify(torrentMock).onReceivedBlock(fileSetMock, 5, 0x3800, new byte[] { 0x00 });
		verify(peerMock).addStrike(-1);
	}

	@Test
	public void testWrite() {
		MessageBlock cut = new MessageBlock(1, 2, new byte[] { 0x00, 0x00, 0x00 });

		byte[] expectedOutput = new byte[] {
				0x00, 0x00, 0x00, 0x01,
				0x00, 0x00, 0x00, 0x02,
				0x00, 0x00, 0x00
		};

		OutStream outStream = new OutStream();
		cut.write(outStream);

		assertEquals(12 ,cut.getLength(), "Incorrect message length");
		assertArrayEquals(expectedOutput, outStream.toByteArray(), "Incorrect output");
	}

}
