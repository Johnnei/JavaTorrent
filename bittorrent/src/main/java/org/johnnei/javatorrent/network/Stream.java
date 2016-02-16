package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

/**
 * A Data Helper Class
 * 
 * <b>Warning</b>: This class is fundamentally broken and is doing far too much.
 * @see InStream
 * @see OutStream
 * 
 * 
 * @author Johnnei
 * 
 */
@Deprecated
public class Stream {
	protected byte[] buffer;
	protected int writeOffset;
	protected int readOffset;

	public Stream() {
		this(5000);
	}

	public Stream(int size) {
		buffer = new byte[size];
	}

	public Stream(byte[] dataBuffer) {
		buffer = dataBuffer;
		writeOffset = buffer.length;
	}
	
	public Stream(byte[] dataBuffer, int offset, int length) {
		buffer = new byte[length];
		System.arraycopy(dataBuffer, offset, buffer, 0, length);
		writeOffset = length;
	}

	public void writeByte(int b) {
		buffer[writeOffset++] = (byte) b;
	}

	public void writeByte(byte[] array) {
		writeByte(array, 0, array.length);
	}
	
	public void writeByte(byte[] array, int offset, int length) {
		for (int i = 0; i < length; i++) {
			writeByte(array[offset + i]);
		}
	}

	public void writeShort(int i) {
		writeByte(i >> 8);
		writeByte(i);
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
		return buffer[readOffset++] & 0xFF;
	}

	public int readInt() {
		return ((readShort() & 0xffff) << 16) | readShort();
	}

	public int readShort() {
		return ((readByte() & 0xff) << 8) | readByte();
	}

	public long readLong() {
		return ((readInt() & 0xffffffffL) << 32) | readInt();
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

	/**
	 * Resets the stream and resizes it
	 * 
	 * @param size
	 */
	public void reset(int size) {
		writeOffset = 0;
		buffer = new byte[size];
	}
	
	/**
	 * Resets the readpointer to the beginning of the buffer
	 */
	public void resetReadPointer() {
		readOffset = 0;
	}
	
	/**
	 * Resets the writepointer to the beginning of the buffer
	 */
	public void resetWritePointer() {
		writeOffset = 0;
	}

	public DatagramPacket write(InetAddress address, int port) {
		return new DatagramPacket(buffer, 0, writeOffset, address, port);
	}

	public void read(DatagramSocket socket) throws IOException {
		read(socket, 5120);
	}

	public void read(DatagramSocket socket, int size) throws IOException {
		reset(size);
		DatagramPacket dp = new DatagramPacket(buffer, 0, buffer.length);
		socket.receive(dp);
		writeOffset = dp.getLength();
		readOffset = 0;
	}

	/**
	 * Resets the buffer size to that of <b>buffer</b>.length and fill the buffer with <b>buffer</b>
	 * 
	 * @param buffer
	 */
	public void fill(byte[] buffer) {
		writeOffset = buffer.length;
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
		byte[] b = new byte[writeOffset];
		for (int i = 0; i < b.length; i++) {
			b[i] = buffer[i];
		}
		buffer = b;
	}
	
	/**
	 * Drop the read bytes and shift the unread bytes to the left<br/>
	 */
	public void refit() {
		if(readOffset > 0) {
			int available = available();
			for(int i = 0; i < available; i++) {
				buffer[i] = buffer[readOffset + i];
			}
			readOffset -= available;
			writeOffset -= available;
		}
	}
	
	/**
	 * Moves the write pointer<br/>
	 * This allows to write at specific points on the stream
	 * 
	 * @param i The amount of bytes to skip over
	 */
	public void skipWrite(int i) {
		writeOffset += i;
	}

	/**
	 * Moves the pointer back
	 * 
	 * @param amount
	 */
	public void moveBack(int amount) {
		readOffset -= amount;
	}

	/**
	 * Expends the buffer to hold <b>amount</b> extra bytes
	 * 
	 * @param amount The extra size needed
	 */
	public void expand(int amount) {
		byte[] newBuffer = new byte[buffer.length + amount];
		for (int i = 0; i < buffer.length; i++) {
			newBuffer[i] = buffer[i];
		}
		buffer = newBuffer;
	}
	
	/**
	 * Gets the amount of bytes which can still be written before the buffer is full
	 * @return
	 */
	public int writeableSpace() {
		return buffer.length - writeOffset;
	}

	/**
	 * Gets the amount of bytes which have not yet been read
	 * @return
	 */
	public int available() {
		return writeOffset - readOffset;
	}

	/**
	 * The raw bytes in the stream
	 * 
	 * @return
	 */
	public byte[] getBuffer() {
		return buffer;
	}
	
	/**
	 * The raw bytes in the stream up to the {@link #writeOffset}
	 * @return
	 */
	public byte[] getWrittenBuffer() {
		byte[] data = new byte[writeOffset];
		System.arraycopy(buffer, 0, data, 0, writeOffset);
		return data;
	}

	/**
	 * The current offset pointer which point to the current offset in the buffer at which you can write
	 * 
	 * @return
	 */
	public int getWritePointer() {
		return writeOffset;
	}
	
	/**
	 * The current offset pointer which point to the current offset in the buffer at which you can read
	 * @return
	 */
	public int getReadPointer() {
		return readOffset;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append("[");
		sb.append("ReadOffset = ");
		sb.append(readOffset);
		sb.append(", WriteOffset = ");
		sb.append(writeOffset);
		sb.append(", Buffer = [");
		if (buffer != null) {
			for (int i = 0; i < buffer.length; i++) {
				boolean hasNext = i + 1 < buffer.length;
				
				String hex = Integer.toHexString(buffer[i] & 0xFF);
				if (hex.length() == 1) {
					sb.append("0");
				}
				
				sb.append(hex);
				
				if (hasNext) {
					sb.append(", ");
				}
			}
		}
		sb.append("] ]");
		
		return sb.toString();
	}
}
