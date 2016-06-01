package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;
import java.io.OutputStream;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.payload.DataPayload;

/**
 * Manages the sending of {@link DataPayload}s to the socket.
 */
public class UtpOutputStream extends OutputStream {

	private UtpSocketImpl socket;

	private byte[] outBuffer;

	private int position;

	public UtpOutputStream(UtpSocketImpl socket) {
		this.socket = socket;
	}

	@Override
	public void write(int b) throws IOException {
		write(new byte[] { (byte) b }, 0, 1);
	}

	@Override
	public void write(byte[] buffer, int offset, int length) throws IOException {
		if (socket.getConnectionState().isClosedState()) {
			throw new IOException("Socket has been closed.");
		}

		ensureBufferSize();
		int remainingBytes = length;
		int readBytes = 0;
		while (remainingBytes > 0) {
			int chunkLength = Math.min(outBuffer.length - position, remainingBytes);
			System.arraycopy(buffer, offset + readBytes, outBuffer, position, chunkLength);
			position += chunkLength;
			readBytes += chunkLength;
			remainingBytes -= chunkLength;

			if (position >= outBuffer.length) {
				sendBuffer();
			}
		}
	}

	@Override
	public void flush() throws IOException {
		if (position == 0) {
			// Don't send packets without data.
			return;
		}

		sendBuffer();
	}

	private void sendBuffer() throws IOException {
		byte[] outBufferClone = new byte[position];
		System.arraycopy(outBuffer, 0, outBufferClone, 0, outBufferClone.length);

		socket.send(new DataPayload(outBufferClone));
		position = 0;
	}

	private void ensureBufferSize() {
		if (outBuffer == null) {
			outBuffer = new byte[socket.getPacketSize()];
			return;
		}

		if (outBuffer.length < socket.getPacketSize()){
			byte[] newBuffer = new byte[socket.getPacketSize()];
			System.arraycopy(outBuffer, 0, newBuffer, 0, outBuffer.length);
			outBuffer = newBuffer;
		}
	}
}
