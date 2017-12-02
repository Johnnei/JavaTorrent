package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link BencodedString}
 */
public class BencodedStringTest {

	@Test
	public void testAsString() throws Exception {
		BencodedString cut = new BencodedString("Hello World!");

		assertEquals("Hello World!", cut.asString(), "asString produced different result.");
	}

	@Test
	public void testAsBytes() throws Exception {
		byte[] input = new byte[] { 'H', 'e', 'l', 'l', 'e', '!' };
		BencodedString cut = new BencodedString(input);

		assertArrayEquals(input, cut.asBytes());
	}

	@ParameterizedTest
	@MethodSource("unsupportedOperations")
	public void testUnsupportedMethods(Consumer<BencodedString> consumer) {
		assertThrows(UnsupportedOperationException.class, () -> consumer.accept(new BencodedString("")));
	}

	public static Stream<Consumer<BencodedString>> unsupportedOperations() {
		return Stream.of(
			BencodedString::asLong,
			BencodedString::asBigInteger,
			BencodedString::asMap,
			BencodedString::asList
		);
	}

	@Test
	public void testSerialize() throws Exception {
		BencodedString cut = new BencodedString("Hello World!");

		assertEquals("12:Hello World!", new String(cut.serialize(), BitTorrent.DEFAULT_ENCODING), "Incorrect serialized form");
	}

	@Test
	public void testSerializeWithNonAsciiCharacters() throws Exception {
		BencodedString cut = new BencodedString("μTorrent 3.4.7");

		assertEquals("15:μTorrent 3.4.7", new String(cut.serialize(), BitTorrent.DEFAULT_ENCODING), "Incorrect serialized form");
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

		assertArrayEquals(expectedSerializedForm, cut.serialize(), "Invalid UTF-8 sequence are allowed to occur within BencodedStrings");
	}

	@Test
	public void testToString() throws Exception {
		BencodedString cut = new BencodedString("test");

		assertTrue(cut.toString().startsWith("BencodedString["), "Incorrect toString() start");
	}
}
