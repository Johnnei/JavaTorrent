package org.johnnei.javatorrent.bittorrent.encoding;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigInteger;

/**
 * A decoder for Bencoding.
 *
 * @see IBencodedValue
 */
public class Bencoding {

	/**
	 * Decodes the given string reader into the bencoded data structure.
	 * @param reader The reader containing the string input.
	 * @return The bencoded data structure.
	 */
	public IBencodedValue decode(StringReader reader) {
		try {
			return decodeNextValue(reader);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to decode bencoded values.", e);
		}
	}

	private IBencodedValue decodeNextValue(StringReader reader) throws IOException {
		char token = peekCharacter(reader);

		IBencodedValue value;
		if ('i' == token) {
			value = decodeInteger(reader);
		} else if ('l' == token) {
			value = decodeList(reader);
		} else if ('d' == token) {
			value = decodeMap(reader);
		} else {
			value = decodeString(reader);
		}

		return value;
	}

	private BencodedList decodeList(StringReader reader) throws IOException {
		consumeToken('l', reader);

		BencodedList list = new BencodedList();

		char token = peekCharacter(reader);
		while ('e' != token) {
			list.add(decodeNextValue(reader));

			token = peekCharacter(reader);
		}

		consumeToken('e', reader);

		return list;
	}

	private BencodedString decodeString(StringReader reader) throws IOException {
		StringBuilder length = new StringBuilder();
		char token = peekCharacter(reader);
		while (':' != token) {
			length.append((char) reader.read());

			token = peekCharacter(reader);
		}

		consumeToken(':', reader);

		char[] characters = new char[Integer.parseInt(length.toString())];
		int totalRead = 0;

		do {
			int read = reader.read(characters, totalRead, characters.length - totalRead);

			if (read <= 0) {
				throw new IOException("Failed to read string");
			}

			totalRead += read;
		} while (totalRead != characters.length);

		return new BencodedString(new String(characters));
	}

	private BencodedMap decodeMap(StringReader reader) throws IOException {
		BencodedMap map = new BencodedMap();

		consumeToken('d', reader);

		char nextToken = peekCharacter(reader);
		while ('e' != nextToken) {
			BencodedString key = decodeString(reader);
			IBencodedValue value = decodeNextValue(reader);
			map.put(key.asString(), value);

			nextToken = peekCharacter(reader);
		}

		consumeToken('e', reader);

		return map;
	}

	private BencodedInteger decodeInteger(StringReader reader) throws IOException {
		consumeToken('i', reader);

		StringBuilder integer = new StringBuilder();

		char nextToken = peekCharacter(reader);
		while ('e' != nextToken) {
			integer.append((char) reader.read());

			nextToken = peekCharacter(reader);
		}

		consumeToken('e', reader);

		return new BencodedInteger(new BigInteger(integer.toString()));
	}

	private char peekCharacter(StringReader reader) throws IOException {
		reader.mark(0);
		char result = (char) reader.read();
		reader.reset();
		return result;
	}

	private void consumeToken(char token, StringReader reader) throws IOException {
		char readToken = (char) reader.read();
		if (token != readToken) {
			throw new IOException(String.format("Incorrect token consumed, expected '%s' but read '%s'", token, readToken));
		}
	}

}
