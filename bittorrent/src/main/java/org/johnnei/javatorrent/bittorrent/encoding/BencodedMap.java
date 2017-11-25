package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

/**
 * A bencoded dictionary.
 */
public class BencodedMap extends AbstractBencodedValue {

	private static final byte[] ENTRY_START_BYTES = "d".getBytes(BitTorrent.DEFAULT_ENCODING);

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
	public byte[] serialize() {
		// For performance reasons we now sort the values.
		// The used comparator is using a lot of encoding to resolve the bytes values which is expensive to do very often.
		// And only at this we absolutely need them sorted, so now do so.
		// TODO Consider if adding a cached byte-array could improve speed.
		Map<String, IBencodedValue> sortedMap = new TreeMap<>(new RawStringComparator());
		sortedMap.putAll(map);

		Entry[] entries = new Entry[sortedMap.size()];
		int entryByteCount = 0;

		int index = 0;
		for (Map.Entry<String, IBencodedValue> entry : sortedMap.entrySet()) {
			entries[index] = new Entry(new BencodedString(entry.getKey()).serialize(), entry.getValue().serialize());
			entryByteCount += entries[index].keyBytes.length + entries[index].valueBytes.length;
			index++;
		}

		ByteBuffer buffer = ByteBuffer.wrap(new byte[ENTRY_START_BYTES.length + ENTRY_END_BYTES.length + entryByteCount]);
		buffer.put(ENTRY_START_BYTES);
		for (Entry entry : entries) {
			buffer.put(entry.keyBytes);
			buffer.put(entry.valueBytes);
		}
		buffer.put(ENTRY_END_BYTES);

		return buffer.array();

	}

	private static class Entry {

		final byte[] keyBytes;

		final byte[] valueBytes;

		Entry(byte[] keyBytes, byte[] valueBytes) {
			this.keyBytes = keyBytes;
			this.valueBytes = valueBytes;
		}
	}

	/**
	 * A comparator that compares strings according to their byte values in UTF-8.
	 */
	private static class RawStringComparator implements Comparator<String> {

		@Override
		public int compare(String a, String b) {
			byte[] aBytes = a.getBytes(BitTorrent.DEFAULT_ENCODING);
			byte[] bBytes = b.getBytes(BitTorrent.DEFAULT_ENCODING);

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
