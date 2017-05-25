package org.johnnei.javatorrent.internal.utp.stream;

import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.Queue;

import org.johnnei.javatorrent.internal.utp.UtpSocket;

public class UtpOutputStream extends OutputStream {

	private UtpSocket socket;

	private Queue<ByteBuffer> bufferedData;

	public UtpOutputStream(UtpSocket socket) {
		this.socket = socket;
		bufferedData = new LinkedList<>();
	}

	@Override
	public void write(int b) {
		ByteBuffer buffer = ByteBuffer.allocate(1);
		buffer.put((byte) (b & 0xFF));
		buffer.flip();
		bufferedData.add(buffer);
		packgePayloads(false);
	}

	@Override
	public void write(byte[] b) {
		bufferedData.add(ByteBuffer.wrap(b));
		packgePayloads(false);
	}

	/**
	 * Causes all buffered data to be immediately enqueued for sending.
	 */
	@Override
	public void flush() {
		packgePayloads(true);
	}

	private void packgePayloads(boolean flush) {
		int payloadSize = socket.getPacketPayloadSize();
		while (hasEnoughBufferedData(payloadSize, flush)) {
			ByteBuffer payloadBuffer = createPayloadBuffer(Math.min(payloadSize, countBufferedBytes()));
			socket.send(payloadBuffer);

			// Refresh the payload size to prevent creating packets which are larger than the current payload size.
			payloadSize = socket.getPacketPayloadSize();
		}
	}

	private boolean hasEnoughBufferedData(int payloadSize, boolean flush) {
		return !bufferedData.isEmpty() && (flush || countBufferedBytes() >= payloadSize);

	}

	private ByteBuffer createPayloadBuffer(int payloadSize) {
		ByteBuffer payloadBuffer = ByteBuffer.allocate(payloadSize);

		while (payloadBuffer.hasRemaining()) {
			ByteBuffer buffer = bufferedData.peek();

			buffer.limit(Math.min(buffer.remaining(), payloadBuffer.remaining()));
			payloadBuffer.put(buffer);
			buffer.limit(buffer.capacity());
			if (!buffer.hasRemaining()) {
				bufferedData.poll();
			}
		}

		payloadBuffer.flip();

		return payloadBuffer;
	}

	private int countBufferedBytes() {
		return bufferedData.stream().map(ByteBuffer::remaining).reduce(0, (a, b) -> a + b);
	}
}
