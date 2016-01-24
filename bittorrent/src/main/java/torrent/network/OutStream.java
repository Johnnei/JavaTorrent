package torrent.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class OutStream {
	
	private ByteArrayOutputStream buffer;
	private DataOutputStream out;
	
	public OutStream() {
		this(32);
	}
	
	public OutStream(int size) {
		buffer = new ByteArrayOutputStream(size);
		out = new DataOutputStream(buffer);
	}

	public void write(int b) {
		try {
			out.write(b);
		} catch (IOException e) { /* Ignore */ }
	}

	public void write(byte[] b) {
		try {
			out.write(b);
		} catch (IOException e) { /* Ignore */ }
	}

	public void write(byte[] b, int off, int len) {
		try {
			out.write(b, off, len);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeBoolean(boolean v) {
		try {
			out.writeBoolean(v);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeByte(int v) {
		try {
			out.writeByte(v);
		} catch (IOException e) { /* Ignore */ }
	}
	
	public void writeByte(byte[] data) {
		write(data);
	}
	
	public void writeByte(byte[] data, int offset, int length) {
		write(data, offset, length);
	}

	public void writeBytes(String s) {
		try {
			out.writeBytes(s);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeChar(int v) {
		try {
			out.writeChar(v);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeChars(String s) {
		try {
			out.writeChars(s);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeDouble(double v) {
		try {
			out.writeDouble(v);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeFloat(float v) {
		try {
			out.writeFloat(v);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeInt(int v) {
		try {
			out.writeInt(v);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeLong(long v) {
		try {
			out.writeLong(v);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeShort(int v) {
		try {
			out.writeShort(v);
		} catch (IOException e) { /* Ignore */ }
	}

	public void writeUTF(String s) {
		try {
			out.writeUTF(s);
		} catch (IOException e) { /* Ignore */ }
	}
	
	public void writeString(String s) {
		for (int i = 0; i < s.length(); i++) {
			writeByte(s.charAt(i));
		}
	}
	
	public int size() {
		return buffer.size();
	}
	
	public byte[] toByteArray() {
		return buffer.toByteArray();
	}

}
