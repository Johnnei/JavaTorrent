package org.johnnei.javatorrent.bittorrent.encoding;

import java.math.BigInteger;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.johnnei.javatorrent.test.TestUtils.assertEqualityMethods;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link BencodedInteger}
 */
public class BencodedIntegerTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testLong() {
		BencodedInteger integer = new BencodedInteger(35);

		assertEquals("asBigInteger returned a different value", BigInteger.valueOf(35), integer.asBigInteger());
		assertEquals("asLong returned a different value", 35L, integer.asLong());
		assertEquals("Serialized form is incorrect", "i35e", integer.serialize());
	}

	@Test
	public void testBigInteger() {
		BencodedInteger integer = new BencodedInteger(BigInteger.valueOf(35));

		assertEquals("asBigInteger returned a different value", BigInteger.valueOf(35), integer.asBigInteger());
		assertEquals("asLong returned a different value", 35L, integer.asLong());
		assertEquals("Serialized form is incorrect", "i35e", integer.serialize());
	}

	@Test
	public void testBigIntegerOutOfLongRange() {
		BencodedInteger integer = new BencodedInteger(new BigInteger("12345678987654321012"));

		assertEquals("asBigInteger returned a different value", new BigInteger("12345678987654321012"), integer.asBigInteger());
		assertEquals("Serialized form is incorrect", "i12345678987654321012e", integer.serialize());
	}

	@Test
	public void testBigIntegerNegativeOutOfLongRange() {
		BencodedInteger integer = new BencodedInteger(new BigInteger("-12345678987654321012"));

		assertEquals("asBigInteger returned a different value", new BigInteger("-12345678987654321012"), integer.asBigInteger());
		assertEquals("Serialized form is incorrect", "i-12345678987654321012e", integer.serialize());
	}

	@Test
	public void testBigIntegerNegativeOutOfLongRangeAsLong() {
		thrown.expect(UnsupportedOperationException.class);
		thrown.expectMessage("out of range");

		BencodedInteger integer = new BencodedInteger(new BigInteger("-12345678987654321012"));
		integer.asLong();
	}

	@Test
	public void testBigIntegerOutOfLongRangeAsLong() {
		thrown.expect(UnsupportedOperationException.class);
		thrown.expectMessage("out of range");

		BencodedInteger integer = new BencodedInteger(new BigInteger("12345678987654321012"));
		integer.asLong();
	}

	@Test
	public void testAsString() {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedInteger(42L).asString();
	}

	@Test
	public void testAsList() {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedInteger(42L).asList();
	}

	@Test
	public void testAsMap() {
		thrown.expect(UnsupportedOperationException.class);

		new BencodedInteger(42L).asMap();
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
		assertTrue("Incorrect toString start", new BencodedInteger(42L).toString().startsWith("BencodedInteger["));
	}

}