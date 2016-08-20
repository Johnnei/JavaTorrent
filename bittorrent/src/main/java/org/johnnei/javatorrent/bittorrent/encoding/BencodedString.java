package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

/**
 * A bencoded string.
 */
public class BencodedString extends AbstractBencodedValue {

	private static final byte[] SEPARATOR_BYTES = ":".getBytes(BitTorrent.DEFAULT_ENCODING);

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
		return new String(value, UTF8);
	}

	@Override
	public byte[] asBytes() {
		return Arrays.copyOf(value, value.length);
	}

	@Override
	public byte[] serialize() {
		byte[] lengthBytes = Integer.toString(value.length).getBytes(BitTorrent.DEFAULT_ENCODING);

		ByteBuffer buffer = ByteBuffer.wrap(new byte[lengthBytes.length + SEPARATOR_BYTES.length + value.length]);
		buffer.put(lengthBytes);
		buffer.put(SEPARATOR_BYTES);
		buffer.put(value);

		return buffer.array();
	}

	@Override
	public String toString() {
		return String.format("BencodedString[value=%s]", asString());
	}
}
