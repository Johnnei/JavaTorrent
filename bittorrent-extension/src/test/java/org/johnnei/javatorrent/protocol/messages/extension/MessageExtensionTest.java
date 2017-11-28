package org.johnnei.javatorrent.protocol.messages.extension;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;
import org.johnnei.javatorrent.protocol.extension.ExtensionModule;
import org.johnnei.javatorrent.test.StubExtension;
import org.johnnei.javatorrent.test.StubMessage;
import org.johnnei.javatorrent.torrent.peer.Peer;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageExtensionTest {

	@Test
	public void testProtocolId() {
		MessageExtension cut = new MessageExtension(new ExtensionModule.Builder().build());
		assertEquals(20, cut.getId(), "Incorrect ID");
		assertTrue(cut.toString().startsWith("MessageExtension["), "Incorrect toString start");
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
		assertEquals(0, inStream.available(), "Not all input was read.");
	}

	@Test
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
		assertThrows(IllegalArgumentException.class, () -> cut.read(inStream));
	}

	@Test
	public void testProcess() {
		Peer peerMock = mock(Peer.class);
		MessageExtension cutOne = new MessageExtension(1, null);
		MessageExtension cutTwo = new MessageExtension(1, new StubMessage());

		when(peerMock.getTorrent()).thenReturn(null);

		cutOne.process(peerMock);
		cutTwo.process(peerMock);
	}

	@Test
	public void testReadHandshake() {
		ExtensionModule moduleMock = mock(ExtensionModule.class);
		MessageExtension cut = new MessageExtension(moduleMock);

		byte[] input = new byte[] {
				// Extension ID
				0x00
		};

		when(moduleMock.createHandshakeMessage()).thenReturn(new MessageHandshake(Collections.emptyList()));

		cut.read(new InStream(input));
	}

	@Test
	public void testWrite() {
		final byte[] expectedOutput = new byte[] { 0x01, 0x01 };
		MessageExtension cut = new MessageExtension(1, new StubMessage());
		assertEquals(3, cut.getLength(), "Incorrect message size");

		OutStream outStream = new OutStream();
		cut.write(outStream);

		// The message ID is not included in this call
		assertEquals(2, outStream.size(), "Incorrect output size");
		assertArrayEquals(expectedOutput, outStream.toByteArray(), "Incorrect output data");
	}

}
