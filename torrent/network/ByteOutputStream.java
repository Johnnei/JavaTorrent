package torrent.network;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteOutputStream extends DataOutputStream {

	private int speed;

	public ByteOutputStream(OutputStream outStream) {
		super(outStream);
		speed = 0;
	}

	public void write(int i) throws IOException {
		speed++;
		super.write(i);
	}

	public void write(byte[] bytes, int offset, int length) throws IOException {
		for (int i = 0; i < length; i++) {
			write(bytes[offset + i]);
		}
	}

	public void writeString(String s) throws IOException {
		for (int i = 0; i < s.length(); i++) {
			write(s.charAt(i) & 0xFF);
		}
	}

	public int getSpeed() {
		return speed;
	}

	public void reset(int uploadRate) {
		speed -= uploadRate;
	}

}
