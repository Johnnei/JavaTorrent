package torrent.network;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteInputStream extends DataInputStream {
	
	/**
	 * The speed in bytes that this inputStream is being read
	 */
	private int speed;
	
	private OutStream buffer;
	private int bufferSize;
	
	/**
	 * The last time a buffer was created
	 */
	private long lastBufferCreate;
	
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
	
	public boolean canReadBufferedMessage() throws IOException {
		if (buffer == null) {
			if (available() < 4) {
				return false;
			}
			
			int length = readInt();
			buffer = new OutStream(length + 4);
			bufferSize = length + 4;
			buffer.writeInt(length);
		}
		
		int remainingBytes = bufferSize - buffer.size();
		if (remainingBytes == 0) {
			return true;
		}
		
		int availableBytes = Math.min(remainingBytes, available());
		buffer.write(readByteArray(availableBytes));
		
		return bufferSize - buffer.size() == 0;
	}
	
	public InStream getBufferedMessage() {
		InStream inStream = new InStream(buffer.toByteArray());
		buffer = null;
		return inStream;
	}

	/**
	 * The time in milliseconds that this buffer has existed
	 * 
	 * @return
	 */
	public int getBufferLifetime() {
		return (int) (System.currentTimeMillis() - lastBufferCreate);
	}
	
	/**
	 * Gets the timestamp at which the last byte has been read
	 * @return
	 */
	public long getLastActivity() {
		return lastActivity;
	}

}
