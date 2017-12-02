package org.johnnei.javatorrent.network.socket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.test.TestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


/**
 * Tests {@link TcpSocket}
 */
public class TcpSocketTest {

	private TcpSocket cut;

	private Socket socketMock;

	@BeforeEach
	public void setUp() {
		socketMock = mock(Socket.class);
		cut = new TcpSocket(socketMock);
	}

	@Test
	public void testClose() throws Exception {
		cut.close();

		verify(socketMock).close();
	}

	@Test
	public void testGetStreams() throws Exception {
		InputStream inputStream = new ByteArrayInputStream(new byte[0]);
		OutputStream outputStream = new ByteArrayOutputStream();

		when(socketMock.getInputStream()).thenReturn(inputStream);
		when(socketMock.getOutputStream()).thenReturn(outputStream);

		assertEquals(inputStream, cut.getInputStream(), "Incorrect inputstream");
		assertEquals(outputStream, cut.getOutputStream(), "Incorrect outputstream");
	}

	@Test
	public void testIsInputShutdown() {
		when(socketMock.isInputShutdown()).thenReturn(true);
		assertTrue(cut.isInputShutdown());
	}

	@Test
	public void testDefaultConstructor() {
		TcpSocket socket = new TcpSocket();
		assertNotNull(Whitebox.getInternalState(socket, Socket.class));
	}

	@Test
	public void testConnect() throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 27960);

		cut.connect(socketAddress);

		verify(socketMock).connect(same(socketAddress), eq(10_000));
	}

	@Test
	public void testEqualsAndHashcode() throws Exception {
		Socket mockOne = mock(Socket.class);
		Socket mockTwo = mock(Socket.class);

		TcpSocket socketOne = new TcpSocket(mockOne);
		TcpSocket socketTwo = new TcpSocket(mockOne);
		TcpSocket socketThree = new TcpSocket(mockTwo);

		when(mockOne.getRemoteSocketAddress()).thenReturn(new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		TestUtils.assertEqualityMethods(socketOne, socketTwo, socketThree);

		assertTrue(socketOne.toString().startsWith("TcpSocket["), "Incorrect toString start");
	}

	@Test
	public void testFlush() throws Exception {
		OutputStream outputStreamMock = mock(OutputStream.class);

		when(socketMock.getOutputStream()).thenReturn(outputStreamMock);

		cut.flush();

		verify(outputStreamMock).flush();
	}

	@Test
	public void testIsOutputShutdown() {
		when(socketMock.isOutputShutdown()).thenReturn(true);

		assertTrue(cut.isOutputShutdown());
	}

	@Test
	public void testIsClosed() {
		when(socketMock.isClosed()).thenReturn(false, true, false);
		when(socketMock.isConnected()).thenReturn(false, true);

		assertTrue(cut.isClosed());
		assertTrue(cut.isClosed());
		assertFalse(cut.isClosed());
	}

}
