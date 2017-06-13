package org.johnnei.javatorrent.internal.utp.stream;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public class UtpInputStream extends InputStream {

	private short nextSequenceNumber;

	private Map<Short, ByteBuffer> sequenceToBuffer;

	public UtpInputStream(short nextSequenceNumber) {
		this.nextSequenceNumber = nextSequenceNumber;
		sequenceToBuffer = new HashMap<>();
	}

	public void submitData(short sequenceNumber, byte[] data) {
		sequenceToBuffer.put(sequenceNumber, ByteBuffer.wrap(data));
	}

	@Override
	public int available() {
		int available = 0;
		short sequenceNumber = nextSequenceNumber;
		while (sequenceToBuffer.containsKey(sequenceNumber)) {
			available += sequenceToBuffer.get(sequenceNumber).remaining();
			sequenceNumber++;
		}

		return available;
	}

	@Override
	public int read() {
		ByteBuffer buffer = sequenceToBuffer.get(nextSequenceNumber);
		byte b = buffer.get();
		if (buffer.remaining() == 0) {
			sequenceToBuffer.remove(nextSequenceNumber);
			nextSequenceNumber++;
		}

		return b;
	}
}
