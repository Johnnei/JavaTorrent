package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A bencoded dictionary.
 */
public class BencodedMap extends AbstractBencodedValue {

	private static final Charset UTF8 = Charset.forName("UTF-8");

	private Map<String, IBencodedValue> map;

	/**
	 * Creates a new empty bencoded map.
	 */
	public BencodedMap() {
		map = new HashMap<>();
	}

	/**
	 * Associates the given bencoded value with the given key. This will overwrite existing entries.
	 * @param name The key to assign the value to.
	 * @param value The value to assign.
	 */
	public void put(String name, IBencodedValue value) {
		map.put(name, value);
	}

	/**
	 * Gets the value from the map if available.
	 * @param name The key to fetch the value for.
	 * @return The value if present.
	 */
	public Optional<IBencodedValue> get(String name) {
		return Optional.ofNullable(map.get(name));
	}

	/**
	 * Removes a value from the map.
	 * @param name The key to remove the value from.
	 * @return The removed value (if present).
	 */
	public Optional<IBencodedValue> remove(String name) {
		return Optional.ofNullable(map.remove(name));
	}

	@Override
	public Map<String, IBencodedValue> asMap() {
		return Collections.unmodifiableMap(map);
	}

	@Override
	public String serialize() {
		// For performance reasons we now sort the values.
		// The used comparator is using a lot of encoding to resolve the bytes values which is expensive to do very often.
		// And only at this we absolutely need them sorted, so now do so.
		// TODO Consider if adding a cached byte-array could improve speed.
		Map<String, IBencodedValue> sortedMap = new TreeMap<>(new RawStringComparator());
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
	 * A comparator that compares strings according to their byte values in UTF-8.
	 */
	private static class RawStringComparator implements Comparator<String> {

		@Override
		public int compare(String a, String b) {
			byte[] aBytes = a.getBytes(UTF8);
			byte[] bBytes = b.getBytes(UTF8);

			int lengthCompare = Integer.compare(aBytes.length, bBytes.length);
			if (lengthCompare != 0) {
				return lengthCompare;
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
		}
	}
}
