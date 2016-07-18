package org.johnnei.javatorrent.bittorrent.encoding;

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

		assertEquals("Incorrect serialized form", "12:Hello World!", cut.serialize());
	}

	@Test
	public void testSerializeWithNonAsciiCharacters() throws Exception {
		BencodedString cut = new BencodedString("μTorrent 3.4.7");

		assertEquals("Incorrect serialized form", "15:μTorrent 3.4.7", cut.serialize());
	}

	@Test
	public void testToString() throws Exception {
		BencodedString cut = new BencodedString("test");

		assertTrue("Incorrect toString() start", cut.toString().startsWith("BencodedString["));
	}
}