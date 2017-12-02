package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link MessageHave}
 */
public class MessageHaveTest {

	@Test
	public void testWrite() {
		byte[] expectedOutput = new byte[] { 0x00, 0x00, 0x00, 0x12 };

		OutStream outStream = new OutStream();

		MessageHave cut = new MessageHave(0x12);
		cut.write(outStream);

		assertArrayEquals(expectedOutput, outStream.toByteArray(), "Incorrect output");
	}

	@Test
	public void testReadAndProcess() {
		Peer peerMock = mock(Peer.class);

		InStream inStream = new InStream(new byte[] { 0x00, 0x00, 0x00, 0x12 });
		MessageHave cut = new MessageHave();
		cut.read(inStream);
		cut.process(peerMock);

		verify(peerMock).setHavingPiece(0x12);
	}

	@Test
	public void testStaticMethods() {
		MessageHave cut = new MessageHave();

		assertEquals(4 ,cut.getId(), "Incorrect message ID");
		assertEquals(5 ,cut.getLength(), "Incorrect message length");
		assertTrue(cut.toString().startsWith("MessageHave["), "Incorrect toString start.");
	}

}
