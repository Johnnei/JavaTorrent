package org.johnnei.javatorrent.network;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.johnnei.javatorrent.internal.utils.CheckedRunnable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(OutStream.class);

	private ByteArrayOutputStream buffer;
	private DataOutputStream out;

	public OutStream() {
		this(32);
	}

	public OutStream(int size) {
		buffer = new ByteArrayOutputStream(size);
		out = new DataOutputStream(buffer);
	}

	public void write(byte[] b) {
		writeUnchecked(() -> out.write(b));
	}

	public void write(byte[] b, int off, int len) {
		writeUnchecked(() -> out.write(b, off, len));
	}

	public void writeBoolean(boolean v) {
		writeUnchecked(() -> out.writeBoolean(v));
	}

	public void writeByte(int v) {
		writeUnchecked(() -> out.writeByte(v));
	}

	public void writeInt(int v) {
		writeUnchecked(() -> out.writeInt(v));
	}

	public void writeLong(long v) {
		writeUnchecked(() -> out.writeLong(v));
	}

	public void writeShort(int v) {
		writeUnchecked(() -> out.writeShort(v));
	}

	public void writeString(String s) {
		write(s.getBytes(Charset.forName("UTF-8")));
	}

	public int size() {
		return buffer.size();
	}

	public byte[] toByteArray() {
		return buffer.toByteArray();
	}

	private void writeUnchecked(CheckedRunnable<IOException> writeCall) {
		try {
			writeCall.run();
		} catch (IOException e) {
			LOGGER.error("Somehow you managed to get an IO exception on a byte array. I'm proud.", e);
			throw new RuntimeException("IO Exception on in-memory byte array.", e);
		}
	}

}
