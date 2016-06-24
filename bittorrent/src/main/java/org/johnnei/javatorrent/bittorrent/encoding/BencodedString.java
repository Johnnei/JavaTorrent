package org.johnnei.javatorrent.bittorrent.encoding;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

/**
 * A bencoded string.
 */
public class BencodedString implements IBencodedValue {

	private String value;

	public BencodedString(String value) {
		this.value = value;
	}

	@Override
	public String asString() {
		return value;
	}

	@Override
	public long asLong() {
		throw new UnsupportedOperationException("String cannot be converted to long");
	}

	@Override
	public BigInteger asBigInteger() {
		throw new UnsupportedOperationException("String cannot be converted to big integer");
	}

	@Override
	public Map<String, IBencodedValue> asMap() {
		throw new UnsupportedOperationException("String cannot be converted to map");
	}

	@Override
	public List<IBencodedValue> asList() {
		throw new UnsupportedOperationException("String cannot be converted to list");
	}

	@Override
	public String serialize() {
		return String.format("%d:%s", value.length(), value);
	}
}
