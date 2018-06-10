package org.johnnei.javatorrent.internal.utp.stream;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.HashMap;
import java.util.Map;

public class InputPacketSorter {

	private final Pipe.SinkChannel inputSink;

	private short nextSequenceNumber;

	private Map<Short, ByteBuffer> sequenceToBuffer;

	public InputPacketSorter(Pipe.SinkChannel inputSink, short nextSequenceNumber) {
		this.inputSink = inputSink;
		this.nextSequenceNumber = nextSequenceNumber;
		sequenceToBuffer = new HashMap<>();
	}

	public void submitData(short sequenceNumber, byte[] data) {
		try {
			sequenceToBuffer.put(sequenceNumber, ByteBuffer.wrap(data));

			while (sequenceToBuffer.containsKey(nextSequenceNumber)) {
				ByteBuffer buffer = sequenceToBuffer.get(nextSequenceNumber);
				inputSink.write(buffer);

				if (buffer.hasRemaining()) {
					break;
				}

				sequenceToBuffer.remove(nextSequenceNumber);
				nextSequenceNumber++;
			}
		} catch (IOException e) {
			throw new IllegalStateException("Failed to pass ST_DATA to input sink.", e);
		}
	}

	public boolean isCompleteUntil(short sequenceNumber) {
		if (nextSequenceNumber == sequenceNumber) {
			return true;
		}

		short seq = nextSequenceNumber;
		while (seq < sequenceNumber) {
			if (!sequenceToBuffer.containsKey(seq)) {
				return false;
			}

			seq++;
		}

		return true;
	}
}
