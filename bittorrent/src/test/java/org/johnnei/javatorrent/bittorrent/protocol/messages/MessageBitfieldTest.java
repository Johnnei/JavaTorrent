package org.johnnei.javatorrent.bittorrent.protocol.messages;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.torrent.peer.Peer;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.eq;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link MessageBitfield}
 */
@RunWith(EasyMockRunner.class)
public class MessageBitfieldTest extends EasyMockSupport {

	@Test
	public void testWrite() {
		byte[] expectedBytes = { 1, 9, 3 };
		OutStream outStream = new OutStream();

		MessageBitfield cut = new MessageBitfield(expectedBytes);

		cut.write(outStream);

		assertEquals("Incorrect message length", 1 + expectedBytes.length, cut.getLength());
		assertArrayEquals("Incorrect output", expectedBytes, outStream.toByteArray());
		assertEquals("Incorrect message ID", 5, cut.getId());
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageBitfield["));
	}

	@Test
	public void testReadAndProcess() {
		byte[] input = { (byte) 0xFF, 0, (byte) 0xF };
		InStream inStream = new InStream(input);

		Peer peerMock = createMock(Peer.class);

		MessageBitfield cut = new MessageBitfield();
		cut.read(inStream);

		for (int i = 0; i < 8; i++) {
			peerMock.havePiece(eq(i));
		}

		for (int i = 0; i < 4; i++) {
			peerMock.havePiece(eq(20 + i));
		}

		replayAll();

		cut.process(peerMock);

		verifyAll();
	}

}