package org.johnnei.javatorrent.network.socket;

import java.net.InetSocketAddress;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpMultiplexer;

import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpSocket}
 */
public class UtpSocketTest {

	private UtpSocket cut;

	private UtpSocketImpl.Builder socketFactoryMock;
	private UtpSocketImpl socketMock;
	private UtpMultiplexer multiplexerMock;

	@Before
	public void setUp() {
		socketMock = mock(UtpSocketImpl.class);
		socketFactoryMock = mock(UtpSocketImpl.Builder.class);
		multiplexerMock = mock(UtpMultiplexer.class);

		cut = new UtpSocket(multiplexerMock);
		Whitebox.setInternalState(cut, UtpSocketImpl.class, socketMock);
		Whitebox.setInternalState(cut, UtpSocketImpl.Builder.class, socketFactoryMock);
	}

	@Test
	public void testConnect() throws Exception {
		when(socketFactoryMock.build()).thenReturn(socketMock);
		when(multiplexerMock.registerSocket(same(socketMock))).thenReturn(false).thenReturn(true);

		InetSocketAddress socketAddress = new InetSocketAddress("localhost", 27960);
		cut.connect(socketAddress);

		verify(socketMock).connect(same(socketAddress));
	}

	@Test
	public void isClosedWhenUnconnected() throws Exception {
		// Remove the mock
		Whitebox.setInternalState(cut, UtpSocketImpl.class, (UtpSocketImpl) null);

		assertTrue(cut.isClosed());
	}

	@Test
	public void testIsClosedWhenConnecting() throws Exception {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTING);
		assertFalse(cut.isClosed());
	}

	@Test
	public void testIsClosedWhenConnected() throws Exception {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);
		assertFalse(cut.isClosed());
	}

	@Test
	public void testIsClosedWhenDisconnecting() throws Exception {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.DISCONNECTING);
		assertFalse(cut.isClosed());
	}

	@Test
	public void testIsClosedWhenDisconnected() throws Exception {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CLOSED);
		assertTrue(cut.isClosed());
	}

}