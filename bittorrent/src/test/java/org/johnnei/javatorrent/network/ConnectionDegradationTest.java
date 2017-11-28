package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.network.socket.ISocket;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link ConnectionDegradation}
 */
public class ConnectionDegradationTest {

	@Test
	public void testDegradeConnection() {
		ConnectionDegradation cut = new ConnectionDegradation.Builder()
			.registerDefaultConnectionType(SocketTypeOne.class, SocketTypeOne::new)
			.registerDefaultConnectionType(SocketTypeOne.class, SocketTypeOne::new, SocketTypeTwo.class)
			.registerConnectionType(SocketTypeTwo.class, SocketTypeTwo::new)
			.build();

		ISocket preferredSocket = cut.createPreferredSocket();
		assertTrue(preferredSocket instanceof SocketTypeOne, "Incorrect preferred socket");
		Optional<ISocket> fallbackSocket = cut.degradeSocket(preferredSocket);
		assertTrue(fallbackSocket.isPresent(), "Fallback socket should be present");
		assertTrue(fallbackSocket.get() instanceof SocketTypeTwo, "Incorrect fallback socket");

		fallbackSocket = cut.degradeSocket(fallbackSocket.get());
		assertFalse(fallbackSocket.isPresent(), "Second fallback socket shouldn't have been there");

		assertTrue(cut.toString().startsWith("ConnectionDegradation["), "Incorrect toString start");
	}

	@Test
	public void testBadConfiguration() {
		assertThrows(IllegalStateException.class, () -> new ConnectionDegradation.Builder()
			.registerDefaultConnectionType(SocketTypeOne.class, SocketTypeOne::new, SocketTypeTwo.class)
			.registerConnectionType(SocketTypeTwo.class, SocketTypeTwo::new, SocketTypeThree.class)
			.build());
	}

	private static class SocketTypeOne extends ASocketType {
	}

	private static class SocketTypeTwo extends ASocketType {
	}

	private static class SocketTypeThree extends ASocketType {
	}

	private static class ASocketType implements ISocket {

		@Override
		public void connect(InetSocketAddress endpoint) throws IOException {

		}

		@Override
		public InputStream getInputStream() throws IOException {
			return null;
		}

		@Override
		public OutputStream getOutputStream() throws IOException {
			return null;
		}

		@Override
		public void close() throws IOException {

		}

		@Override
		public boolean isClosed() {
			return false;
		}

		@Override
		public boolean isInputShutdown() {
			return false;
		}

		@Override
		public boolean isOutputShutdown() {
			return false;
		}

		@Override
		public void flush() throws IOException {

		}
	}
}
