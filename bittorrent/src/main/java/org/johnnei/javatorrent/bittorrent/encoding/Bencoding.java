package org.johnnei.javatorrent.bittorrent.encoding;

import java.io.IOException;
import java.math.BigInteger;

import org.johnnei.javatorrent.network.InStream;

/**
 * A decoder for Bencoding.
 *
 * @see IBencodedValue
 */
public class Bencoding {

	private int charactersRead;

	/**
	 * Decodes the given string reader into the bencoded data structure.
	 * @param inStream The stream containing the string input.
	 * @return The bencoded data structure.
	 */
	public IBencodedValue decode(InStream inStream) {
		try {
			charactersRead = 0;
			return decodeNextValue(inStream);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to decode bencoded values.", e);
		}
	}

	private IBencodedValue decodeNextValue(InStream inStream) throws IOException {
		char token = peekCharacter(inStream);

		IBencodedValue value;
		if ('i' == token) {
			value = decodeInteger(inStream);
		} else if ('l' == token) {
			value = decodeList(inStream);
		} else if ('d' == token) {
			value = decodeMap(inStream);
		} else {
			value = decodeString(inStream);
		}

		return value;
	}

	private BencodedList decodeList(InStream inStream) throws IOException {
		consumeToken('l', inStream);

		BencodedList list = new BencodedList();

		char token = peekCharacter(inStream);
		while ('e' != token) {
			list.add(decodeNextValue(inStream));

			token = peekCharacter(inStream);
		}

		consumeToken('e', inStream);

		return list;
	}

	private BencodedString decodeString(InStream inStream) throws IOException {
		StringBuilder length = new StringBuilder();
		char token = peekCharacter(inStream);
		while (':' != token) {
			length.append(readCharacter(inStream));

			token = peekCharacter(inStream);
		}

		consumeToken(':', inStream);

		int stringLength = Integer.parseInt(length.toString());

		if (inStream.available() < stringLength) {
			throw new IOException(String.format("Failed to decode Bencoded string. Need %d bytes but only got %d.", stringLength, inStream.available()));
		}

		byte[] stringBytes = inStream.readFully(stringLength);
		charactersRead += stringLength;

		return new BencodedString(stringBytes);
	}

	private BencodedMap decodeMap(InStream inStream) throws IOException {
		BencodedMap map = new BencodedMap();

		consumeToken('d', inStream);

		char nextToken = peekCharacter(inStream);
		while ('e' != nextToken) {
			BencodedString key = decodeString(inStream);
			try {
				IBencodedValue value = decodeNextValue(inStream);
				map.put(key.asString(), value);
			} catch (Exception e) {
				throw new IOException(String.format("Failed to read dictionary value associated with key: %s", key), e);
			}

			nextToken = peekCharacter(inStream);
		}

		consumeToken('e', inStream);

		return map;
	}

	private BencodedInteger decodeInteger(InStream inStream) throws IOException {
		consumeToken('i', inStream);

		StringBuilder integer = new StringBuilder();

		char nextToken = peekCharacter(inStream);
		while ('e' != nextToken) {
			integer.append(readCharacter(inStream));

			nextToken = peekCharacter(inStream);
		}

		consumeToken('e', inStream);

		return new BencodedInteger(new BigInteger(integer.toString()));
	}

	private char readCharacter(InStream inStream) throws IOException {
		if (inStream.available() == 0) {
			throw new IOException("End of Stream reached");
		}

		int character = Byte.toUnsignedInt(inStream.readByte());

		charactersRead++;

		return (char) character;
	}

	private char peekCharacter(InStream inStream) throws IOException {
		inStream.mark();
		char result = (char) Byte.toUnsignedInt(inStream.readByte());
		inStream.resetToMark();
		return result;
	}

	private void consumeToken(char token, InStream inStream) throws IOException {
		char readToken = readCharacter(inStream);
		if (token != readToken) {
			throw new IOException(String.format("Incorrect token consumed, expected '%s' but read '%s'", token, readToken));
		}
	}

}
