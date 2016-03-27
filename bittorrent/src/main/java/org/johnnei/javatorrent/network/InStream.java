package org.johnnei.javatorrent.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Optional;

import org.johnnei.javatorrent.internal.utils.CheckedRunnable;
import org.johnnei.javatorrent.internal.utils.CheckedSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(InStream.class);

	private Duration readDuration;

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
	 * Calls <code>InStream(data, 0, data.length, null)</code>.
	 * @param data The byte buffer
	 *
	 * @see #InStream(byte[], int, int)
	 */
	public InStream(byte[] data) {
		this(data, 0, data.length, null);
	}

	/**
	 * Creates a new buffered input stream based on the given byte array.
	 *
	 * Calls <code>InStream(data, 0, data.length, readDuration)</code>.
	 * @param data The byte buffer
	 * @param readDuration The duration it took to read the given buffer
	 *
	 * @see #InStream(byte[], int, int, Duration)
	 */
	public InStream(byte[] data, Duration readDuration) {
		this(data, 0, data.length, readDuration);
	}

	/**
	 * Creates a new buffered input stream based on the section in the given byte array.
	 * @param data The byte buffer
	 * @param offset The starting offset
	 * @param length The amount of bytes available
	 *
	 * @see #InStream(byte[], int, int, Duration)
	 */
	public InStream(byte[] data, int offset, int length) {
		this(data, offset, length, null);
	}

	/**
	 * Creates a new buffered input stream based on the section in the given byte array.
	 * @param data The byte buffer
	 * @param offset The starting offset
	 * @param length The amount of bytes available
	 * @param readDuration The duration it took to read the given buffer
	 */
	public InStream(byte[] data, int offset, int length, Duration readDuration) {
		buffer = new ByteArrayInputStream(data, offset, length);
		in = new DataInputStream(buffer);
		this.length = length;
		this.readDuration = readDuration;
	}

	/**
	 * Reads a boolean value from the stream. The boolean is <code>true</code> when the read byte is not zero.
	 * @return The read boolean
	 */
	public boolean readBoolean() {
		return doUnchecked(() -> in.readBoolean());
	}

	/**
	 * Reads a signed byte from the stream.
	 * @return The read byte.
	 */
	public byte readByte() {
		return doUnchecked(() -> in.readByte());
	}

	/**
	 * Reads a character from the stream.
	 * @return The read character.
	 */
	public char readChar() {
		return doUnchecked(() -> in.readChar());
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
		doUnchecked(() -> in.readFully(b, off, len));
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
		return doUnchecked(() -> in.readInt());
	}

	/**
	 * Reads a long from the stream.
	 * @return The read long.
	 */
	public long readLong() {
		return doUnchecked(() -> in.readLong());
	}

	/**
	 * Reads a short from the stream.
	 * @return The read short.
	 */
	public short readShort() {
		return doUnchecked(() -> in.readShort());
	}

	/**
	 * Reads an unsigned byte from the stream.
	 * @return The read byte.
	 */
	public int readUnsignedByte() {
		return doUnchecked(() -> in.readUnsignedByte());
	}

	/**
	 * Reads an unsigned short from the stream.
	 * @return The read short.
	 */
	public int readUnsignedShort() {
		return doUnchecked(() -> in.readUnsignedShort());
	}

	/**
	 * Skips the next <code>n</code> bytes on the stream.
	 * @param n The amount of bytes to skip
	 * @return The actual amount of bytes skipped.
	 */
	public int skipBytes(int n) {
		return doUnchecked(() -> in.skipBytes(n));
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
		return doUnchecked(() -> in.available());
	}

	/**
	 * Reads a string from the stream.
	 * @param length The amount of characters in the string
	 * @return The read string.
	 */
	public String readString(int length) {
		byte[] stringBytes = readFully(length);
		return new String(stringBytes, Charset.forName("UTF-8"));
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

	/**
	 * Gets the duration it took to read this message.
	 * @return The duration it took or {@link Optional#empty()} if unknown
	 */
	public Optional<Duration> getReadDuration() {
		return Optional.ofNullable(readDuration);
	}

	private void doUnchecked(CheckedRunnable<IOException> readCall) {
		try {
			readCall.run();
		} catch (IOException e) {
			LOGGER.error("You managed to cause an IO exception on an in-memory byte array. I'm proud.", e);
			throw new RuntimeException("IO Exception on in-memory byte array", e);
		}
	}

	private <T> T doUnchecked(CheckedSupplier<T, IOException> readCall) {
		try {
			return readCall.get();
		} catch (IOException e) {
			LOGGER.error("You managed to cause an IO exception on an in-memory byte array. I'm proud.", e);
			throw new RuntimeException("IO Exception on in-memory byte array", e);
		}
	}

}
