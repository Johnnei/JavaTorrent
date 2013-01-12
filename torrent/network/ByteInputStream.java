package torrent.network;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.johnnei.utils.ThreadUtils;

import torrent.download.peer.Peer;

public class ByteInputStream extends DataInputStream {

	private Peer peer;
	private int speed;

	public ByteInputStream(Peer peer, InputStream in) {
		super(in);
		this.peer = peer;
		speed = 0;
	}

	public int read() throws IOException {
		while (available() == 0) {
			if (System.currentTimeMillis() - peer.getLastActivity() > 30000) // 30 Second Read time-out
				throw new IOException("Read timed out");
			ThreadUtils.sleep(1);
		}
		int b = super.read();
		if (b != -1) {
			++speed;
			peer.updateLastActivity();
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

}
