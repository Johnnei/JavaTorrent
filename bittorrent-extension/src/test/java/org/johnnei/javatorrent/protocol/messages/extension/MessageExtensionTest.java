package org.johnnei.javatorrent.protocol.messages.extension;

import java.time.Duration;
import java.util.Collections;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.test.StubExtension;
import org.johnnei.javatorrent.test.StubMessage;
import org.johnnei.javatorrent.torrent.download.peer.Peer;

import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(EasyMockRunner.class)
public class MessageExtensionTest extends EasyMockSupport {

	@Test
	public void testProtocolId() {
		MessageExtension cut = new MessageExtension(new ExtensionModule.Builder().build());
		assertEquals("Incorrect ID", 20, cut.getId());
		assertTrue("Incorrect toString start", cut.toString().startsWith("MessageExtension["));
	}

	@Test
	public void testRead() {
		ExtensionModule module = new ExtensionModule.Builder()
				.registerExtension(new StubExtension("jt_stub"))
				.build();
		MessageExtension cut = new MessageExtension(module);

		byte[] input = new byte[] {
				// Extension ID
				0x01,
				// Payload
				0x01
		};

		InStream inStream = new InStream(input);
		cut.read(inStream);

		// Stub message also asserts the actual input
		assertEquals("Not all input was read.", 0, inStream.available());
	}

	@Test(expected=IllegalArgumentException.class)
	public void testReadInvalidId() {
		ExtensionModule module = new ExtensionModule.Builder()
				.registerExtension(new StubExtension("jt_stub"))
				.build();
		MessageExtension cut = new MessageExtension(module);

		byte[] input = new byte[] {
				// Extension ID
				0x02,
				// Payload
				0x01
		};

		InStream inStream = new InStream(input);
		cut.read(inStream);
	}

	@Test
	public void testProcess() {
		Peer peerMock = createMock(Peer.class);
		MessageExtension cutOne = new MessageExtension(1, null);
		MessageExtension cutTwo = new MessageExtension(1, new StubMessage());

		expect(peerMock.getTorrent()).andReturn(null);

		replayAll();

		cutOne.process(peerMock);
		cutTwo.process(peerMock);

		verifyAll();
	}

	@Test
	public void testReadHandshake() {
		ExtensionModule moduleMock = createMock(ExtensionModule.class);
		MessageExtension cut = new MessageExtension(moduleMock);

		byte[] input = new byte[] {
				// Extension ID
				0x00
		};

		expect(moduleMock.createHandshakeMessage()).andReturn(new MessageHandshake(Collections.emptyList()));

		replayAll();

		cut.read(new InStream(input));
		cut.setReadDuration(Duration.ZERO);

		verifyAll();
	}

	@Test
	public void testWrite() {
		final byte[] expectedOutput = new byte[] { 0x01, 0x01 };
		MessageExtension cut = new MessageExtension(1, new StubMessage());
		assertEquals("Incorrect message size", 3, cut.getLength());

		OutStream outStream = new OutStream();
		cut.write(outStream);

		// The message ID is not included in this call
		assertEquals("Incorrect output size", 2, outStream.size());
		assertArrayEquals("Incorrect output data", expectedOutput, outStream.toByteArray());
	}

}
