package org.johnnei.javatorrent.bittorrent.encoding;

import java.math.BigInteger;
import java.util.Objects;

/**
 * A bencoded integer.
 */
public class BencodedInteger extends AbstractBencodedValue {

	private static final BigInteger MAX_LONG_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

	private static final BigInteger MIN_LONG_VALUE = BigInteger.valueOf(Long.MIN_VALUE);

	private BigInteger bigInteger;

	/**
	 * Creates a new bencoded long.
	 * @param integerValue The long to bencode.
	 */
	public BencodedInteger(long integerValue) {
		this(BigInteger.valueOf(integerValue));
	}

	/**
	 * Creates a new bencoded integer.
	 * @param bigInteger The integer to bencode.
	 */
	public BencodedInteger(BigInteger bigInteger) {
		this.bigInteger = bigInteger;
	}

	@Override
	public long asLong() {
		if (bigInteger.compareTo(MAX_LONG_VALUE) > 0 || bigInteger.compareTo(MIN_LONG_VALUE) < 0) {
			throw new UnsupportedOperationException("Integer value is out of range to fit in a long.");
		}

		return bigInteger.longValueExact();
	}

	@Override
	public BigInteger asBigInteger() {
		return bigInteger;
	}

	@Override
	public String serialize() {
		return String.format("i%se", bigInteger.toString());
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}

		if (this == o) {
			return true;
		}

		if (!(o instanceof BencodedInteger)) {
			return false;
		}

		BencodedInteger integer = (BencodedInteger) o;
		return Objects.equals(bigInteger, integer.bigInteger);
	}

	@Override
	public int hashCode() {
		return Objects.hash(bigInteger);
	}

	@Override
	public String toString() {
		return String.format("BencodedInteger[value=%s]", bigInteger);
	}
}
