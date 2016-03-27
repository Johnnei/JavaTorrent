package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Optional;

import org.johnnei.javatorrent.network.socket.ISocket;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link ConnectionDegradation}
 */
public class ConnectionDegradationTest {

	@Test
	public void testDegradeConnection() {
		ConnectionDegradation cut = new ConnectionDegradation.Builder()
				.registerDefaultConnectionType(SocketTypeOne.class, SocketTypeOne::new, Optional.empty())
				.registerDefaultConnectionType(SocketTypeOne.class, SocketTypeOne::new, Optional.of(SocketTypeTwo.class))
				.registerConnectionType(SocketTypeTwo.class, SocketTypeTwo::new, Optional.empty())
				.build();

		ISocket preferredSocket = cut.createPreferredSocket();
		assertTrue("Incorrect preferred socket", preferredSocket instanceof SocketTypeOne);
		Optional<ISocket> fallbackSocket = cut.degradeSocket(preferredSocket);
		assertTrue("Fallback socket should be present", fallbackSocket.isPresent());
		assertTrue("Incorrect fallback socket", fallbackSocket.get() instanceof SocketTypeTwo);

		fallbackSocket = cut.degradeSocket(fallbackSocket.get());
		assertFalse("Second fallback socket shouldn't have been there", fallbackSocket.isPresent());

		assertTrue("Incorrect toString start", cut.toString().startsWith("ConnectionDegradation["));
	}

	@Test(expected = IllegalStateException.class)
	public void testBadConfiguration() {
		new ConnectionDegradation.Builder()
				.registerDefaultConnectionType(SocketTypeOne.class, SocketTypeOne::new, Optional.of(SocketTypeTwo.class))
				.registerConnectionType(SocketTypeTwo.class, SocketTypeTwo::new, Optional.of(SocketTypeThree.class))
				.build();
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
		public boolean isConnecting() {
			return false;
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