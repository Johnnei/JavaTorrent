package org.johnnei.javatorrent.bittorrent.encoding;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

/**
 * Base class for implementations for {@link IBencodedValue} which implements all <code>asX</code> methods to throw {@link UnsupportedOperationException}
 */
public abstract class AbstractBencodedValue implements IBencodedValue {

	protected static final byte[] ENTRY_END_BYTES = "e".getBytes(BitTorrent.DEFAULT_ENCODING);

	@Override
	public String asString() {
		throw new UnsupportedOperationException("Bencoded value cannot be converted to string");
	}

	@Override
	public byte[] asBytes() {
		throw new UnsupportedOperationException("Bencoded value cannot be converted to string");
	}

	@Override
	public Map<String, IBencodedValue> asMap() {
		throw new UnsupportedOperationException("Bencoded value cannot be converted to map");
	}

	@Override
	public List<IBencodedValue> asList() {
		throw new UnsupportedOperationException("Bencoded value cannot be converted to list");
	}

	@Override
	public long asLong() {
		throw new UnsupportedOperationException("Bencoded value cannot be converted to long");
	}

	@Override
	public BigInteger asBigInteger() {
		throw new UnsupportedOperationException("Bencoded value cannot be converted to big integer");
	}
}
