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

	public BencodedList() {
		bencodedValues = new ArrayList<>();
	}

	public IBencodedValue get(int index) {
		return bencodedValues.get(index);
	}

	public void add(IBencodedValue value) {
		bencodedValues.add(value);
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
