package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * A bencoded string.
 */
public class BencodedString extends AbstractBencodedValue {

	private byte[] value;

	/**
	 * Creates a new bencoded string based on the given value.
	 * @param value The string to bencode.
	 */
	public BencodedString(String value) {
		this.value = value.getBytes(Charset.forName("UTF-8"));
	}

	/**
	 * Creates a new bencoded string based on the given value.
	 * @param value The string to bencode.
	 */
	public BencodedString(byte[] value) {
		this.value = value;
	}

	@Override
	public String asString() {
		return new String(value);
	}

	@Override
	public byte[] asBytes() {
		return Arrays.copyOf(value, value.length);
	}

	@Override
	public String serialize() {
		return String.format("%d:%s", value.length, asString());
	}

	@Override
	public String toString() {
		return String.format("BencodedString[value=%s]", value);
	}
}
