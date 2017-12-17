package org.johnnei.javatorrent.internal.network.connector;

import java.nio.ByteBuffer;
import java.time.Clock;
import java.time.Instant;

public class HandshakeState {

	private final ByteBuffer handshakeBuffer = ByteBuffer.allocate(BitTorrentHandshakeHandlerImpl.HANDSHAKE_SIZE);

	private final Instant connectionStart;

	private final byte[] expectedTorrent;

	public HandshakeState(Clock clock, byte[] expectedTorrent) {
		this.expectedTorrent = expectedTorrent;
		connectionStart = clock.instant();
	}

	public ByteBuffer getHandshakeBuffer() {
		return handshakeBuffer;
	}

	public Instant getConnectionStart() {
		return connectionStart;
	}

	public byte[] getExpectedTorrent() {
		return expectedTorrent;
	}
}
