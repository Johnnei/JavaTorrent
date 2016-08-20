package org.johnnei.javatorrent.bittorrent.encoding;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * Represents a value which was bencoded.
 */
public interface IBencodedValue {

	/**
	 * @return The bencoded value as a string.
	 * @throws UnsupportedOperationException When the stored type is not a string.
	 */
	String asString();

	/**
	 * @return The bencoded value as a byte array.
	 * @throws UnsupportedOperationException When the stored type is not a string.
	 */
	byte[] asBytes();

	/**
	 * @return The bencoded value as a long.
	 * @throws UnsupportedOperationException When the stored type is not a long or overflows a long.
	 */
	long asLong();

	/**
	 * @return The bencoded value as a big integer.
	 * @throws UnsupportedOperationException When the stored type is not a big integer.
	 */
	BigInteger asBigInteger();

	/**
	 * @return The bencoded value as a map.
	 * @throws UnsupportedOperationException When the stored type is not a map.
	 */
	Map<String, IBencodedValue> asMap();

	/**
	 * @return The bencoded value as a list.
	 * @throws UnsupportedOperationException When the stored type is not a list.
	 */
	List<IBencodedValue> asList();

	/**
	 * @return The value represented in bencoded format.
	 */
	byte[] serialize();
}
