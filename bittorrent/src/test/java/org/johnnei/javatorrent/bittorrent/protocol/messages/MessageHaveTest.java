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
 * Tests {@link MessageHave}
 */
@RunWith(EasyMockRunner.class)
public class MessageHaveTest extends EasyMockSupport {

	@Test
	public void testWrite() {
		byte[] expectedOutput = new byte[] { 0x00, 0x00, 0x00, 0x12 };

		OutStream outStream = new OutStream();

		MessageHave cut = new MessageHave(0x12);
		cut.write(outStream);

		assertArrayEquals("Incorrect output", expectedOutput, outStream.toByteArray());
	}

	@Test
	public void testReadAndProcess() {
		Peer peerMock = createMock(Peer.class);
		peerMock.setHavingPiece(eq(0x12));

		replayAll();

		InStream inStream = new InStream(new byte[] { 0x00, 0x00, 0x00, 0x12 });
		MessageHave cut = new MessageHave();
		cut.read(inStream);
		cut.process(peerMock);

		verifyAll();
	}

	@Test
	public void testStaticMethods() {
		MessageHave cut = new MessageHave();

		assertEquals("Incorrect message ID", 4 ,cut.getId());
		assertEquals("Incorrect message length", 5 ,cut.getLength());
		assertTrue("Incorrect toString start.", cut.toString().startsWith("MessageHave["));
	}

}