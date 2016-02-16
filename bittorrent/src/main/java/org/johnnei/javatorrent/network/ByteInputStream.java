package org.johnnei.javatorrent.network;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteInputStream extends DataInputStream {
	
	/**
	 * The speed in bytes that this inputStream is being read
	 */
	private int speed;
	
	/**
	 * The timestamp of the last time we've succesfully read a byte.
	 */
	private long lastActivity;

	public ByteInputStream(InputStream in) {
		super(in);
		speed = 0;
	}

	@Override
	public int read() throws IOException {
		int b = super.read();
		if (b != -1) {
			++speed;
			lastActivity = System.currentTimeMillis();
		}
		return b;
	}

	public String readString(int length) throws IOException {
		String s = "";
		while (length > 0) {
			s += (char) (read() & 0xFF);
			length--;
		}
		return s;
	}

	public String readString() throws IOException {
		return readString(read());
	}

	public byte[] readByteArray(int length) throws IOException {
		byte[] array = new byte[length];
		for (int i = 0; i < array.length; i++) {
			array[i] = (byte) (read() & 0xFF);
		}
		return array;
	}

	public int getSpeed() {
		return speed;
	}

	public void reset(int downloadRate) {
		speed -= downloadRate;
	}

	/**
	 * Gets the timestamp at which the last byte has been read
	 * @return
	 */
	public long getLastActivity() {
		return lastActivity;
	}

}
