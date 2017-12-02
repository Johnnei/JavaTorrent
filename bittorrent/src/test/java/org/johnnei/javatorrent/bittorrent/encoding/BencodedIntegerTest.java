package org.johnnei.javatorrent.bittorrent.encoding;

import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.johnnei.javatorrent.test.TestUtils.assertEqualityMethods;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link BencodedInteger}
 */
public class BencodedIntegerTest {

	@Test
	public void testLong() {
		BencodedInteger integer = new BencodedInteger(35);

		assertEquals(BigInteger.valueOf(35), integer.asBigInteger(), "asBigInteger returned a different value");
		assertEquals(35L, integer.asLong(), "asLong returned a different value");
		assertEquals("i35e", new String(integer.serialize(), BitTorrent.DEFAULT_ENCODING), "Serialized form is incorrect");
	}

	@Test
	public void testBigInteger() {
		BencodedInteger integer = new BencodedInteger(BigInteger.valueOf(35));

		assertEquals(BigInteger.valueOf(35), integer.asBigInteger(), "asBigInteger returned a different value");
		assertEquals(35L, integer.asLong(), "asLong returned a different value");
		assertEquals("i35e", new String(integer.serialize(), BitTorrent.DEFAULT_ENCODING), "Serialized form is incorrect");
	}

	@Test
	public void testBigIntegerOutOfLongRange() {
		BencodedInteger integer = new BencodedInteger(new BigInteger("12345678987654321012"));

		assertEquals(new BigInteger("12345678987654321012"), integer.asBigInteger(), "asBigInteger returned a different value");
		assertEquals("i12345678987654321012e", new String(integer.serialize(), BitTorrent.DEFAULT_ENCODING), "Serialized form is incorrect");
	}

	@Test
	public void testBigIntegerNegativeOutOfLongRange() {
		BencodedInteger integer = new BencodedInteger(new BigInteger("-12345678987654321012"));

		assertEquals(new BigInteger("-12345678987654321012"), integer.asBigInteger(), "asBigInteger returned a different value");
		assertEquals("i-12345678987654321012e", new String(integer.serialize(), BitTorrent.DEFAULT_ENCODING), "Serialized form is incorrect");
	}

	@ParameterizedTest
	@ValueSource(strings = { "-12345678987654321012", "12345678987654321012" })
	public void testLongOutOfRange() {
		BencodedInteger integer = new BencodedInteger(new BigInteger("12345678987654321012"));
		Exception e = assertThrows(UnsupportedOperationException.class, integer::asLong);
		assertThat(e.getMessage(), containsString("out of range"));
	}

	@ParameterizedTest
	@MethodSource("unsupportedOperations")
	public void testUnsupportedMethods(Consumer<BencodedInteger> consumer) {
		assertThrows(UnsupportedOperationException.class, () -> consumer.accept(new BencodedInteger(42L)));
	}

	public static Stream<Consumer<BencodedInteger>> unsupportedOperations() {
		return Stream.of(
			BencodedInteger::asBytes,
			BencodedInteger::asString,
			BencodedInteger::asList,
			BencodedInteger::asMap
		);
	}

	@Test
	public void testEquality() {
		BencodedInteger base = new BencodedInteger(42);
		BencodedInteger equal = new BencodedInteger(BigInteger.valueOf(42));
		BencodedInteger different = new BencodedInteger(7);

		assertEqualityMethods(base, equal, different);
	}

	@Test
	public void testToString() {
		assertTrue(new BencodedInteger(42L).toString().startsWith("BencodedInteger["), "Incorrect toString start");
	}

}
