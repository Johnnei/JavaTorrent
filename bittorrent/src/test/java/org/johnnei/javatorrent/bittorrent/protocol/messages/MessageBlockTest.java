package org.johnnei.javatorrent.bittorrent.protocol.messages;

import java.time.Duration;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.Torrent;
import org.johnnei.javatorrent.torrent.TorrentFileSet;
import org.johnnei.javatorrent.torrent.files.Piece;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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

	@Test
	public void testStaticMethods() {
		MessageBlock cut = new MessageBlock();

		assertEquals("Incorrect message ID", 7 ,cut.getId());
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageBlock["));

		cut = new MessageBlock(1, 2, new byte[] {});
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageBlock["));
	}

	private void prepareTest() {
		when(peerMock.getTorrent()).thenReturn(torrentMock);
		when(torrentMock.getFileSet()).thenReturn(fileSetMock);
		when(pieceMock.getIndex()).thenReturn(5);
		when(fileSetMock.getPiece(5)).thenReturn(pieceMock);
	}

	@Test
	public void testProcessQuickReceive() {
		InStream inStream = new InStream(new byte[] {
				0x00, 0x00, 0x00, 0x05,
				0x00, 0x00, 0x38, 0x00,
				0x00
		}, Duration.ofMillis(200));

		prepareTest();

		MessageBlock cut = new MessageBlock();
		cut.read(inStream);
		cut.process(peerMock);

		verify(torrentMock).onReceivedBlock(fileSetMock, 5, 0x3800, new byte[] { 0x00 });
		verify(peerMock).onReceivedBlock(pieceMock, 0x3800);
		verify(peerMock).addStrike(-1);
		verify(peerMock).setRequestLimit(5);
	}

	@Test
	public void testProcessInvalidLength() {
		prepareTest();

		MessageBlock cut = new MessageBlock(5, 0x37FF, new byte[] {});
		cut.process(peerMock);

		verify(peerMock).onReceivedBlock(pieceMock, 0x37FF);
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
		verify(peerMock).onReceivedBlock(pieceMock, 0x3800);
		verify(peerMock).addStrike(-1);
		verify(peerMock).setRequestLimit(6);
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
		verify(peerMock).onReceivedBlock(pieceMock, 0x3800);
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

		assertEquals("Incorrect message length", 12 ,cut.getLength());
		assertArrayEquals("Incorrect output", expectedOutput, outStream.toByteArray());
	}

}