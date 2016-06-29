package org.johnnei.javatorrent.internal.network;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteInputStream extends DataInputStream {

	/**
	 * The speed in bytes that this inputStream is being read
	 */
	private int speed;

	public ByteInputStream(InputStream in) {
		super(in);
		speed = 0;
	}

	@Override
	public int read() throws IOException {
		int b = super.read();
		if (b != -1) {
			++speed;
		}
		return b;
	}

	public String readString(int length) throws IOException {
		String s = "";
		int remainingBytes = length;
		while (remainingBytes > 0) {
			s += Character.toString((char) (read() & 0xFF));
			remainingBytes--;
		}
		return s;
	}

	public byte[] readByteArray(int length) throws IOException {
		byte[] array = new byte[length];
		readFully(array);
		speed += length;
		return array;
	}

	public int pollSpeed() {
		int polledSpeed = speed;
		speed -= polledSpeed;
		return polledSpeed;
	}

}
