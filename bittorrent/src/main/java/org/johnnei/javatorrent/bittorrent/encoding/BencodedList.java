package org.johnnei.javatorrent.bittorrent.encoding;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.johnnei.javatorrent.bittorrent.protocol.BitTorrent;

/**
 * A bencoded list.
 */
public class BencodedList extends AbstractBencodedValue {

	private static final byte[] ENTRY_START_BYTES = "l".getBytes(BitTorrent.DEFAULT_ENCODING);

	private List<IBencodedValue> bencodedValues;

	/**
	 * Creates a new empty bencoded list.
	 */
	public BencodedList() {
		bencodedValues = new ArrayList<>();
	}

	/**
	 * Gets an item from the bencoded list.
	 * @param index The index to get the item at.
	 * @return The item at the given index.
	 */
	public IBencodedValue get(int index) {
		return bencodedValues.get(index);
	}

	/**
	 * Adds a value to the bencoded list.
	 * @param value The value to add.
	 */
	public void add(IBencodedValue value) {
		bencodedValues.add(value);
	}

	/**
	 * @return The amount of bencoded items in this list.
	 */
	public int size() {
		return bencodedValues.size();
	}

	@Override
	public List<IBencodedValue> asList() {
		return Collections.unmodifiableList(bencodedValues);
	}

	@Override
	public byte[] serialize() {
		byte[][] serializedValues = new byte[bencodedValues.size()][];
		int serializedValuesByteCount = 0;

		int index = 0;
		for (IBencodedValue bencodedValue : bencodedValues) {
			serializedValues[index] = bencodedValue.serialize();
			serializedValuesByteCount += serializedValues[index].length;
			index++;
		}

		ByteBuffer buffer = ByteBuffer.wrap(new byte[ENTRY_START_BYTES.length + ENTRY_END_BYTES.length + serializedValuesByteCount]);
		buffer.put(ENTRY_START_BYTES);
		for (byte[] serializedEntry : serializedValues) {
			buffer.put(serializedEntry);
		}
		buffer.put(ENTRY_END_BYTES);

		return buffer.array();
	}
}
