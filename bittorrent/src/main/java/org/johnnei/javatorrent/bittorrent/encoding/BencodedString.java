package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.charset.Charset;
import java.util.Arrays;

/**
 * A bencoded string.
 */
public class BencodedString extends AbstractBencodedValue {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private byte[] value;

	/**
	 * Creates a new bencoded string based on the given value.
	 * @param value The string to bencode.
	 */
	public BencodedString(String value) {
		this.value = value.getBytes(UTF8);
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
		String result = new String(value, UTF8);
		assert result.getBytes(UTF8).length == value.length : "Failed to serialise string object.";
		return result;
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
		return String.format("BencodedString[value=%s]", asString());
	}
}
