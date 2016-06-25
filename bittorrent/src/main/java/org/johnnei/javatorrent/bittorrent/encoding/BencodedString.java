package org.johnnei.javatorrent.bittorrent.encoding;

/**
 * A bencoded string.
 */
public class BencodedString extends AbstractBencodedValue {

	private String value;

	/**
	 * Creates a new bencoded string based on the given value.
	 * @param value The string to bencode.
	 */
	public BencodedString(String value) {
		this.value = value;
	}

	@Override
	public String asString() {
		return value;
	}

	@Override
	public String serialize() {
		return String.format("%d:%s", value.length(), value);
	}
}
