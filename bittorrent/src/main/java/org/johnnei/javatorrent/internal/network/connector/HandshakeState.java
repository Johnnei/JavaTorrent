package org.johnnei.javatorrent.internal.network.connector;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;

import org.johnnei.javatorrent.network.socket.ISocket;

public class HandshakeState {

	private final ByteBuffer handshakeBuffer = ByteBuffer.allocate(BitTorrentHandshakeHandlerImpl.HANDSHAKE_SIZE);

	private final ISocket socket;

	private final Instant connectionStart;

	private final byte[] expectedTorrent;

	public HandshakeState(Clock clock, ISocket socket, byte[] expectedTorrent) {
		this.expectedTorrent = expectedTorrent;
		this.socket = socket;
		connectionStart = clock.instant();
	}

	public ByteBuffer getHandshakeBuffer() {
		return handshakeBuffer;
	}

	public ISocket getSocket() {
		return socket;
	}

	public Instant getConnectionStart() {
		return connectionStart;
	}

	public byte[] getExpectedTorrent() {
		return expectedTorrent;
	}
}
