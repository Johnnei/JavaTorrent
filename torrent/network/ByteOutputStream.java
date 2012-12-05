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

	public synchronized void write(int i) throws IOException {
		speed++;
		super.write(i);
	}

	public void writeString(String s) throws IOException {
		for (int i = 0; i < s.length(); i++) {
			write(s.charAt(i) & 0xFF);
		}
	}

	public synchronized int getSpeedAndReset() {
		int speed = this.speed;
		this.speed = 0;
		return speed;
	}

}
