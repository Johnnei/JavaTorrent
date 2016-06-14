package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utils.Sync;
import org.johnnei.javatorrent.internal.utp.protocol.payload.DataPayload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.Short.toUnsignedInt;

/**
 * Ensures that the received {@link DataPayload} are being read in order.
 */
public class UtpInputStream extends InputStream {

	private static final Logger LOGGER = LoggerFactory.getLogger(UtpInputStream.class);

	private final Lock notifyLock = new ReentrantLock();

	private final Condition onPacketArrived = notifyLock.newCondition();

	private UtpSocketImpl socket;

	private short nextSequenceNumber;

	private HashMap<Short, DataPayload> packets;

	private byte[] readBuffer;

	private int position;

	public UtpInputStream(UtpSocketImpl socket, short initialSequenceNumber) {
		this.socket = socket;
		nextSequenceNumber = initialSequenceNumber;
		packets = new HashMap<>();
	}

	public void addToBuffer(short sequenceNumber, DataPayload dataPayload) {
		int expectedSequenceNumber = toUnsignedInt(nextSequenceNumberToReceive());
		packets.putIfAbsent(sequenceNumber, dataPayload);
		LOGGER.trace("Received packet {} (expected: {}). Available: {}", toUnsignedInt(sequenceNumber), expectedSequenceNumber, available());
		Sync.signalAll(notifyLock, onPacketArrived);
	}

	@Override
	public int read() throws IOException {
		if (getAvailableBytesInCurrentBuffer() < 1) {
			readNextBuffer();
		}
		return readBuffer[position++] & 0xFF;
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws IOException {
		// TODO Optimize the reading.
		if (length <= 0) {
			return 0;
		}

		int readLimit = Math.min(length, Math.max(1, available()));
		for (int i = 0; i < readLimit; i++) {
			buffer[offset + i] = (byte) (read() & 0xFF);
		}

		return readLimit;
	}

	private void readNextBuffer() throws IOException {
		DataPayload payload = packets.remove(nextSequenceNumber);

		while (payload == null) {
			if (socket.getConnectionState().isClosedState() && nextSequenceNumber >= socket.getEndOfStreamSequenceNumber()) {
				throw new EOFException("InputStream has been shutdown by the remote end.");
			}

			notifyLock.lock();
			try {
				onPacketArrived.await();
			} catch (InterruptedException e) {
				throw new IOException("Interrupted while blocking for new data", e);
			} finally {
				notifyLock.unlock();
			}
			payload = packets.remove(nextSequenceNumber);
		}

		readBuffer = payload.getData();
		position = 0;
		nextSequenceNumber++;
	}

	private short nextSequenceNumberToReceive() {
		short sequenceNumber = nextSequenceNumber;
		while (packets.get(sequenceNumber) != null) {
			sequenceNumber++;
		}

		return sequenceNumber;
	}

	@Override
	public int available() {
		int sum = getAvailableBytesInCurrentBuffer();
		short sequenceNumber = nextSequenceNumber;
		DataPayload payload;
		while ((payload = packets.get(sequenceNumber)) != null) {
			sum += payload.getData().length;
			sequenceNumber++;
		}

		return sum;
	}

	private int getAvailableBytesInCurrentBuffer() {
		if (readBuffer == null) {
			return 0;
		}

		return readBuffer.length - position;
	}
}
