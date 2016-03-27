package org.johnnei.javatorrent.internal.network;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteOutputStream extends FilterOutputStream {

	private int speed;

	public ByteOutputStream(OutputStream outStream) {
		super(outStream);
		speed = 0;
	}

	@Override
	public void write(int i) throws IOException {
		speed++;
		super.write(i);
	}

	@Override
	public void write(byte[] bytes, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			write(bytes[offset + i]);
		}
	}

	public int pollSpeed() {
		int polledSpeed = speed;
		speed -= polledSpeed;
		return polledSpeed;
	}

	public void writeByte(int i) throws IOException {
		write(i);
	}

	public void writeString(String s) throws IOException {
		for (int i = 0; i < s.length(); i++) {
			write(s.charAt(i) & 0xFF);
		}
	}

}
