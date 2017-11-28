package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link MessageBitfield}
 */
public class MessageBitfieldTest {

	@Test
	public void testWrite() {
		byte[] expectedBytes = { 1, 9, 3 };
		OutStream outStream = new OutStream();

		MessageBitfield cut = new MessageBitfield(expectedBytes);

		cut.write(outStream);

		assertEquals(1 + expectedBytes.length, cut.getLength(), "Incorrect message length");
		assertArrayEquals(expectedBytes, outStream.toByteArray(), "Incorrect output");
		assertEquals(5, cut.getId(), "Incorrect message ID");
		assertTrue(cut.toString().startsWith("MessageBitfield["), "Incorrect toString start.");
	}

	@Test
	public void testReadAndProcess() {
		byte[] input = { (byte) 0xFF, 0, (byte) 0xF };
		InStream inStream = new InStream(input);

		Peer peerMock = mock(Peer.class);

		MessageBitfield cut = new MessageBitfield();
		cut.read(inStream);
		cut.process(peerMock);

		for (int i = 0; i < 8; i++) {
			verify(peerMock).setHavingPiece(eq(i));
		}

		for (int i = 0; i < 4; i++) {
			verify(peerMock).setHavingPiece(eq(20 + i));
		}

	}

}
