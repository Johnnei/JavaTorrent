package org.johnnei.javatorrent.bittorrent.encoding;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

/**
 * A bencoded integer.
 */
public class BencodedInteger extends AbstractBencodedValue {

	private static final BigInteger MAX_LONG_VALUE = BigInteger.valueOf(Long.MAX_VALUE);

	private static final BigInteger MIN_LONG_VALUE = BigInteger.valueOf(Long.MIN_VALUE);

	private static final byte[] ENTRY_START_BYTES = "i".getBytes(BitTorrent.DEFAULT_ENCODING);

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
	public byte[] serialize() {
		byte[] integerBytes = bigInteger.toString().getBytes(BitTorrent.DEFAULT_ENCODING);

		ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[ENTRY_START_BYTES.length + integerBytes.length + ENTRY_END_BYTES.length]);

		byteBuffer.put(ENTRY_START_BYTES);
		byteBuffer.put(integerBytes);
		byteBuffer.put(ENTRY_END_BYTES);

		return byteBuffer.array();
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
