package org.johnnei.javatorrent.bittorrent.encoding;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A bencoded list.
 */
public class BencodedList implements IBencodedValue {

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
	public String asString() {
		throw new UnsupportedOperationException("List cannot be converted to string");
	}

	@Override
	public long asLong() {
		throw new UnsupportedOperationException("List cannot be converted to long");
	}

	@Override
	public BigInteger asBigInteger() {
		throw new UnsupportedOperationException("List cannot be converted to big integer");
	}

	@Override
	public Map<String, IBencodedValue> asMap() {
		throw new UnsupportedOperationException("List cannot be converted to map");
	}

	@Override
	public List<IBencodedValue> asList() {
		return Collections.unmodifiableList(bencodedValues);
	}

	@Override
	public String serialize() {
		StringBuilder bencoded = new StringBuilder("l");
		for (IBencodedValue bencodedValue : bencodedValues) {
			bencoded.append(bencodedValue.serialize());
		}
		bencoded.append("e");
		return bencoded.toString();
	}
}
