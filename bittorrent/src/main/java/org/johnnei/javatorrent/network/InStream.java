package org.johnnei.javatorrent.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

public class InStream {

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

	public InStream(byte[] data) {
		this(data, 0, data.length);
	}

	public InStream(byte[] data, int offset, int length) {
		buffer = new ByteArrayInputStream(data, offset, length);
		in = new DataInputStream(buffer);
		this.length = length;
	}

	public boolean readBoolean() {
		try {
			return in.readBoolean();
		} catch (IOException e) { /* Ignore */ return false; }
	}

	public byte readByte() {
		try {
			return in.readByte();
		} catch (IOException e) { /* Ignore */ return -1; }
	}

	public char readChar() {
		try {
			return in.readChar();
		} catch (IOException e) { /* Ignore */ return 0; }
	}

	public double readDouble() {
		try {
			return in.readDouble();
		} catch (IOException e) { /* Ignore */ return Double.NaN; }
	}

	public float readFloat() {
		try {
			return in.readFloat();
		} catch (IOException e) { /* Ignore */ return Float.NaN; }
	}

	public void readFully(byte[] b) {
		try {
			in.readFully(b);
		} catch (IOException e) { /* Ignore */ }
	}

	public void readFully(byte[] b, int off, int len) {
		try {
			in.readFully(b, off, len);
		} catch (IOException e) { /* Ignore */ }
	}

	public byte[] readFully(int len) {
		byte[] array = new byte[len];
		readFully(array);
		return array;
	}

	public int readInt() {
		try {
			return in.readInt();
		} catch (IOException e) { /* Ignore */ return -1; }
	}

	@Deprecated
	public String readLine() {
		try {
			return in.readLine();
		} catch (IOException e) { /* Ignore */ return null;}
	}

	public long readLong() {
		try {
			return in.readLong();
		} catch (IOException e) { /* Ignore */ return -1; }
	}

	public short readShort() {
		try {
			return in.readShort();
		} catch (IOException e) { /* Ignore */ return -1; }
	}

	public String readUTF() {
		try {
			return in.readUTF();
		} catch (IOException e) { /* Ignore */ return null; }
	}

	public int readUnsignedByte() {
		try {
			return in.readUnsignedByte();
		} catch (IOException e) { /* Ignore */ return 0; }
	}

	public int readUnsignedShort() {
		try {
			return in.readUnsignedShort();
		} catch (IOException e) { /* Ignore */ return 0; }
	}

	public int skipBytes(int n) {
		try {
			return in.skipBytes(n);
		} catch (IOException e) { /* Ignore */ return 0; }
	}

	public void moveBack(int n) {
		int offset = length - buffer.available() - n;
		buffer.reset();
		skipBytes(offset);
	}

	public int available() {
		try {
			return in.available();
		} catch (IOException e) { /* Ignore */ return -1; }
	}

	public String readString(int length) {
		StringBuilder builder = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			builder.append((char) readByte());
		}
		return builder.toString();
	}

	public void mark() {
		buffer.mark(buffer.available());
	}

	public void resetToMark() {
		buffer.reset();
	}

}
