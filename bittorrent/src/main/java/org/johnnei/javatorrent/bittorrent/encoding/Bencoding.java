package org.johnnei.javatorrent.bittorrent.encoding;

import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;

/**
 * A decoder for Bencoding.
 *
 * @see IBencodedValue
 */
public class Bencoding {

	private int charactersRead;

	/**
	 * Decodes the given string reader into the bencoded data structure.
	 * @param reader The reader containing the string input. The implementation <b>must</b> support {@link Reader#markSupported()}
	 * @return The bencoded data structure.
	 */
	public IBencodedValue decode(Reader reader) {
		try {
			charactersRead = 0;
			return decodeNextValue(reader);
		} catch (IOException e) {
			throw new IllegalArgumentException("Failed to decode bencoded values.", e);
		}
	}

	/**
	 * @return The amount of characters read from the reader in the last invocation of {@link #decode(Reader)}.
	 */
	public int getCharactersRead() {
		return charactersRead;
	}

	private IBencodedValue decodeNextValue(Reader reader) throws IOException {
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

	private BencodedList decodeList(Reader reader) throws IOException {
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

	private BencodedString decodeString(Reader reader) throws IOException {
		StringBuilder length = new StringBuilder();
		char token = peekCharacter(reader);
		while (':' != token) {
			length.append(readCharacter(reader));

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

			charactersRead += read;
			totalRead += read;
		} while (totalRead != characters.length);

		return new BencodedString(new String(characters));
	}

	private BencodedMap decodeMap(Reader reader) throws IOException {
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

	private BencodedInteger decodeInteger(Reader reader) throws IOException {
		consumeToken('i', reader);

		StringBuilder integer = new StringBuilder();

		char nextToken = peekCharacter(reader);
		while ('e' != nextToken) {
			integer.append(readCharacter(reader));

			nextToken = peekCharacter(reader);
		}

		consumeToken('e', reader);

		return new BencodedInteger(new BigInteger(integer.toString()));
	}

	private char readCharacter(Reader reader) throws IOException {
		int character = reader.read();
		if (character == -1) {
			throw new IOException("End of Stream reached");
		}

		charactersRead++;

		return (char) character;
	}

	private char peekCharacter(Reader reader) throws IOException {
		reader.mark(1);
		char result = (char) reader.read();
		reader.reset();
		return result;
	}

	private void consumeToken(char token, Reader reader) throws IOException {
		char readToken = readCharacter(reader);
		if (token != readToken) {
			throw new IOException(String.format("Incorrect token consumed, expected '%s' but read '%s'", token, readToken));
		}
	}

}
