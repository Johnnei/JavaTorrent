package org.johnnei.javatorrent.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(InStream.class);

	/**
	 * The byte array reader
	 */
	private ByteArrayInputStream buffer;

	/**
	 * The wrapper around the {@link #buffer} which provides data-type access.
	 */
	private DataInputStream in;

	/**
	 * The total amount of supplied bytes
	 */
	private int length;

	/**
	 * Creates a new buffered input stream based on the given byte array.
	 *
	 * Calls <code>InStream(data, 0, data.length)</code>.
	 * @param data The byte buffer
	 *
	 * @see #InStream(byte[], int, int)
	 */
	public InStream(byte[] data) {
		this(data, 0, data.length);
	}

	/**
	 * Creates a new buffered input stream based on the section in the given byte array.
	 * @param data The byte buffer
	 * @param offset The starting offset
	 * @param length The amount of bytes available
	 */
	public InStream(byte[] data, int offset, int length) {
		buffer = new ByteArrayInputStream(data, offset, length);
		in = new DataInputStream(buffer);
		this.length = length;
	}

	/**
	 * Reads a boolean value from the stream. The boolean is <code>true</code> when the read byte is not zero.
	 * @return The read boolean
	 */
	public boolean readBoolean() {
		try {
			return in.readBoolean();
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return false;
		}
	}

	/**
	 * Reads a signed byte from the stream.
	 * @return The read byte.
	 */
	public byte readByte() {
		try {
			return in.readByte();
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return -1;
		}
	}

	/**
	 * Reads a character from the stream.
	 * @return The read character.
	 */
	public char readChar() {
		try {
			return in.readChar();
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return 0;
		}
	}

	/**
	 * Fills the given byte array with data read from the stream.
	 * @param b The byte buffer to copy the data to.
	 */
	public void readFully(byte[] b) {
		readFully(b, 0, b.length);
	}

	/**
	 * Fills the given section of the byte array with data read from the stream.
	 * @param b The byte buffer to copy the data to.
	 * @param off The starting offset.
	 * @param len The amount of bytes to copy.
	 */
	public void readFully(byte[] b, int off, int len) {
		try {
			in.readFully(b, off, len);
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
		}
	}

	/**
	 * Reads <code>length</code> bytes into a new byte array.
	 * @param length The amount of bytes to read
	 * @return The read bytes.
	 */
	public byte[] readFully(int length) {
		byte[] array = new byte[length];
		readFully(array);
		return array;
	}

	/**
	 * Reads an integer from the stream.
	 * @return The read integer.
	 */
	public int readInt() {
		try {
			return in.readInt();
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return -1;
		}
	}

	/**
	 * Reads a long from the stream.
	 * @return The read long.
	 */
	public long readLong() {
		try {
			return in.readLong();
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return -1;
		}
	}

	/**
	 * Reads a short from the stream.
	 * @return The read short.
	 */
	public short readShort() {
		try {
			return in.readShort();
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return -1;
		}
	}

	/**
	 * Reads an unsigned byte from the stream.
	 * @return The read byte.
	 */
	public int readUnsignedByte() {
		try {
			return in.readUnsignedByte();
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return 0;
		}
	}

	/**
	 * Reads an unsigned short from the stream.
	 * @return The read short.
	 */
	public int readUnsignedShort() {
		try {
			return in.readUnsignedShort();
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return 0;
		}
	}

	/**
	 * Skips the next <code>n</code> bytes on the stream.
	 * @param n The amount of bytes to skip
	 * @return The actual amount of bytes skipped.
	 */
	public int skipBytes(int n) {
		try {
			return in.skipBytes(n);
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return 0;
		}
	}

	/**
	 * Moves the read pointer back with <code>n</code> position.
	 * This method cannot be combined with {@link #mark()} on the same stream as this method assumes the marked position to be 0.
	 * @param n The amount of bytes to 'unread'
	 *
	 * @see #mark()
	 */
	public void moveBack(int n) {
		int offset = length - buffer.available() - n;
		buffer.reset();
		skipBytes(offset);
	}

	/**
	 * Calculates the amount of bytes still remaining to read on the stream.
	 * @return The amount of readable bytes.
	 */
	public int available() {
		try {
			return in.available();
		} catch (IOException e) {
			LOGGER.error("IO Error on in memory buffer", e);
			return -1;
		}
	}

	/**
	 * Reads a string from the stream.
	 * @param length The amount of characters in the string
	 * @return The read string.
	 */
	public String readString(int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append((char) readByte());
		}
		return builder.toString();
	}

	/**
	 * Marks the current position.
	 */
	public void mark() {
		buffer.mark(buffer.available());
	}

	/**
	 * Returns to the last marked position with {@link #mark()} or <code>0</code> if no mark has happened before.
	 */
	public void resetToMark() {
		buffer.reset();
	}

}
