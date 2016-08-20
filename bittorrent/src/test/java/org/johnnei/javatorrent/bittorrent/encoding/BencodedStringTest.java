package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.ByteBuffer;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link BencodedString}
 */
public class BencodedStringTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testAsString() throws Exception {
		BencodedString cut = new BencodedString("Hello World!");

		assertEquals("asString produced different result.", "Hello World!", cut.asString());
	}

	@Test
	public void testAsBytes() throws Exception {
		byte[] input = new byte[] { 'H', 'e', 'l', 'l', 'e', '!' };
		BencodedString cut = new BencodedString(input);

		assertArrayEquals("asBytes produced different result.", input, cut.asBytes());
	}

	@Test
	public void testAsLong() throws Exception {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedString("").asLong();
	}

	@Test
	public void testAsBigInteger() throws Exception {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedString("").asBigInteger();
	}

	@Test
	public void testAsMap() throws Exception {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedString("").asMap();

	}

	@Test
	public void testAsList() throws Exception {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedString("").asList();
	}

	@Test
	public void testSerialize() throws Exception {
		BencodedString cut = new BencodedString("Hello World!");

		assertEquals("Incorrect serialized form", "12:Hello World!", new String(cut.serialize(), BitTorrent.DEFAULT_ENCODING));
	}

	@Test
	public void testSerializeWithNonAsciiCharacters() throws Exception {
		BencodedString cut = new BencodedString("μTorrent 3.4.7");

		assertEquals("Incorrect serialized form", "15:μTorrent 3.4.7", new String(cut.serialize(), BitTorrent.DEFAULT_ENCODING));
	}

	@Test
	public void testSerializeWithInvalidUTF8Sequences() throws Exception {
		byte[] invalidSequence = new byte[] {
				(byte) 0x2d, (byte) 0x4a, (byte) 0x54, (byte) 0x30, (byte) 0x30,
				(byte) 0x31, (byte) 0x31, (byte) 0x2d, (byte) 0xd3, (byte) 0x7f,
				(byte) 0xd6, (byte) 0xb0, (byte) 0xc5, (byte) 0x46, (byte) 0x03,
				(byte) 0x7d, (byte) 0x2e, (byte) 0x0c, (byte) 0xc7, (byte) 0xb9
		};

		byte[] prefixBytes = "20:".getBytes(BitTorrent.DEFAULT_ENCODING);
		byte[] expectedSerializedForm = ByteBuffer.wrap(new byte[prefixBytes.length + invalidSequence.length])
				.put(prefixBytes)
				.put(invalidSequence)
				.array();

		BencodedString cut = new BencodedString(invalidSequence);

		assertArrayEquals("Invalid UTF-8 sequence are allowed to occur within BencodedStrings", expectedSerializedForm, cut.serialize());
	}

	@Test
	public void testToString() throws Exception {
		BencodedString cut = new BencodedString("test");

		assertTrue("Incorrect toString() start", cut.toString().startsWith("BencodedString["));
	}
}