package org.johnnei.javatorrent.bittorrent.encoding;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A bencoded dictionary.
 */
public class BencodedMap implements IBencodedValue {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private Map<String, IBencodedValue> map;

	public BencodedMap() {
		map = new HashMap<>();
	}

	public void put(String name, IBencodedValue value) {
		map.put(name, value);
	}

	public Optional<IBencodedValue> get(String name) {
		return Optional.ofNullable(map.get(name));
	}

	public Optional<IBencodedValue> remove(String name) {
		return Optional.ofNullable(map.remove(name));
	}

	@Override
	public String asString() {
		throw new UnsupportedOperationException("Map cannot be converted to string");
	}

	@Override
	public long asLong() {
		throw new UnsupportedOperationException("Map cannot be converted to long");
	}

	@Override
	public BigInteger asBigInteger() {
		throw new UnsupportedOperationException("Map cannot be converted to big integer");
	}

	@Override
	public Map<String, IBencodedValue> asMap() {
		return Collections.unmodifiableMap(map);
	}

	@Override
	public List<IBencodedValue> asList() {
		throw new UnsupportedOperationException("Map cannot be converted to list");
	}

	@Override
	public String serialize() {
		// For performance reasons we now sort the values.
		// The used comparator is using a lot of encoding to resolve the bytes values which is expensive to do very often.
		// And only at this we absolutely need them sorted, so now do so.
		// TODO Consider if adding a cached byte-array could improve speed.
		Map<String, IBencodedValue> sortedMap = new TreeMap<>(getStringComparator());
		sortedMap.putAll(map);

		StringBuilder bencoded = new StringBuilder("d");
		for (Map.Entry<String, IBencodedValue> entry : sortedMap.entrySet()) {
			bencoded.append(new BencodedString(entry.getKey()).serialize());
			bencoded.append(entry.getValue().serialize());
		}
		bencoded.append("e");
		return bencoded.toString();
	}

	/**
	 * @return A comparator that compares strings according to their byte values in UTF-8.
	 */
	private Comparator<String> getStringComparator() {
		return (a, b) -> {
			byte[] aBytes = a.getBytes(UTF8);
			byte[] bBytes = b.getBytes(UTF8);

			if (aBytes.length != bBytes.length) {
				if (aBytes.length > bBytes.length) {
					return 1;
				} else {
					return -1;
				}
			}

			for (int i = 0; i < aBytes.length; i++) {
				if (aBytes[i] != bBytes[i]) {
					if (aBytes[i] > bBytes[i]) {
						return 1;
					} else {
						return -1;
					}
				}
			}

			return 0;
		};
	}
}
