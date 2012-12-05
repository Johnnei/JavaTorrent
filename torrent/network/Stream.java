package torrent.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

/**
 * A Data Helper Class
 * 
 * @author Johnnei
 * 
 */
public class Stream {
	protected byte[] buffer;
	protected int offset;

	public Stream() {
		this(5000);
	}

	public Stream(int size) {
		buffer = new byte[size];
		offset = 0;
	}

	public void writeByte(int b) {
		buffer[offset++] = (byte) b;
	}

	public void writeByte(byte[] array) {
		for (int i = 0; i < array.length; i++) {
			writeByte(array[i]);
		}
	}

	public void writeInt(int i) {
		writeByte(i >> 24);
		writeByte(i >> 16);
		writeByte(i >> 8);
		writeByte(i);
	}

	public void writeInt(long l) {
		writeInt((int) l);
	}

	public void writeLong(long l) {
		writeInt(l >> 32);
		writeInt(l);
	}

	public void writeIntAsString(int i) {
		writeString(Integer.toString(i));
	}

	public void writeString(String s) {
		for (int i = 0; i < s.length(); i++) {
			writeByte(s.charAt(i));
		}
	}

	public int readByte() {
		return buffer[offset++] & 0xFF;
	}

	public int readInt() {
		return (readByte() << 24) + (readByte() << 16) + (readByte() << 8) + readByte();
	}

	public int readShort() {
		return (readByte() << 8) + readByte();
	}

	public long readLong() {
		return (readInt() << 32) + readInt();
	}

	public byte[] readIP() {
		byte[] address = new byte[4];
		for (int i = 0; i < address.length; i++) {
			address[i] = (byte) (readByte() & 0xFF);
		}
		return address;
	}

	public String readString(int length) {
		String s = "";
		for (int i = 0; i < length; i++) {
			s += (char) readByte();
		}
		return s;
	}

	public String readString() {
		int length = readByte();
		return readString(length);
	}

	/**
	 * Resets the stream and resizes it
	 * 
	 * @param size
	 */
	public void reset(int size) {
		offset = 0;
		buffer = new byte[size];
	}

	public DatagramPacket write(InetAddress address, int port) {
		return new DatagramPacket(buffer, 0, offset, address, port);
	}

	public void read(DatagramSocket socket) throws IOException {
		read(socket, 5120);
	}

	public void read(DatagramSocket socket, int size) throws IOException {
		reset(size);
		DatagramPacket dp = new DatagramPacket(buffer, 0, buffer.length);
		socket.receive(dp);
		byte[] newBuffer = new byte[dp.getLength()];
		for (int i = 0; i < newBuffer.length; i++) {
			newBuffer[i] = buffer[i];
		}
		buffer = newBuffer;
		offset = 0;
	}

	/**
	 * Resets the buffer size to that of <b>buffer</b>.length and fill the buffer with <b>buffer</b>
	 * 
	 * @param buffer
	 */
	public void fill(byte[] buffer) {
		offset = 0;
		this.buffer = buffer;
	}

	public byte[] readByteArray(int length) {
		byte[] array = new byte[length];
		for (int i = 0; i < length; i++) {
			array[i] = (byte) readByte();
		}
		return array;
	}

	/**
	 * Resizes the buffer to the size on which the offset currently is
	 */
	public void fit() {
		byte[] b = new byte[offset];
		for (int i = 0; i < b.length; i++) {
			b[i] = buffer[i];
		}
		buffer = b;
	}

	/**
	 * Moves the pointer back
	 * 
	 * @param amount
	 */
	public void moveBack(int amount) {
		offset -= amount;
	}

	public int available() {
		return buffer.length - offset;
	}

	public byte[] getBuffer() {
		return buffer;
	}
}
