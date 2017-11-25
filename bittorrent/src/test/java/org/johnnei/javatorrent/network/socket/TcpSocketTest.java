package org.johnnei.javatorrent.network.socket;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.test.TestUtils;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link TcpSocket}
 */
public class TcpSocketTest extends EasyMockSupport {

	private TcpSocket cut;

	private Socket socketMock;

	@Before
	public void setUp() {
		socketMock = createMock(Socket.class);
		cut = new TcpSocket(socketMock);
	}

	@Test
	public void testClose() throws Exception {
		socketMock.close();

		replayAll();

		cut.close();

		verifyAll();
	}

	@Test
	public void testGetStreams() throws Exception {
		InputStream inputStream = new ByteArrayInputStream(new byte[0]);
		OutputStream outputStream = new ByteArrayOutputStream();

		expect(socketMock.getInputStream()).andReturn(inputStream);
		expect(socketMock.getOutputStream()).andReturn(outputStream);

		replayAll();

		assertEquals("Incorrect inputstream", inputStream, cut.getInputStream());
		assertEquals("Incorrect outputstream", outputStream, cut.getOutputStream());

		verifyAll();
	}

	@Test
	public void testIsInputShutdown() {
		expect(socketMock.isInputShutdown()).andReturn(true);

		replayAll();

		assertTrue(cut.isInputShutdown());

		verifyAll();
	}

	@Test
	public void testDefaultConstructor() {
		TcpSocket socket = new TcpSocket();
		assertNotNull(Whitebox.getInternalState(socket, Socket.class));
	}

	@Test
	public void testConnect() throws Exception {
		InetSocketAddress socketAddress = new InetSocketAddress(InetAddress.getLocalHost(), 27960);

		socketMock.connect(same(socketAddress), eq(10_000));

		replayAll();

		cut.connect(socketAddress);

		verifyAll();
	}

	@Test
	public void testEqualsAndHashcode() throws Exception {
		Socket mockOne = createMock(Socket.class);
		Socket mockTwo = createMock(Socket.class);

		TcpSocket socketOne = new TcpSocket(mockOne);
		TcpSocket socketTwo = new TcpSocket(mockOne);
		TcpSocket socketThree = new TcpSocket(mockTwo);

		expect(mockOne.getRemoteSocketAddress()).andReturn(new InetSocketAddress(InetAddress.getLocalHost(), 27960));

		replayAll();
		TestUtils.assertEqualityMethods(socketOne, socketTwo, socketThree);

		assertTrue("Incorrect toString start", socketOne.toString().startsWith("TcpSocket["));

		verifyAll();
	}

	@Test
	public void testFlush() throws Exception {
		OutputStream outputStreamMock = createMock(OutputStream.class);

		expect(socketMock.getOutputStream()).andReturn(outputStreamMock);
		outputStreamMock.flush();

		replayAll();

		cut.flush();

		verifyAll();
	}

	@Test
	public void testIsOutputShutdown() {
		expect(socketMock.isOutputShutdown()).andReturn(true);

		replayAll();

		assertTrue(cut.isOutputShutdown());

		verifyAll();
	}

	@Test
	public void testIsClosed() {
		expect(socketMock.isClosed()).andReturn(false);
		expect(socketMock.isConnected()).andReturn(false);

		expect(socketMock.isClosed()).andReturn(true);

		expect(socketMock.isClosed()).andReturn(false);
		expect(socketMock.isConnected()).andReturn(true);

		replayAll();

		assertTrue(cut.isClosed());
		assertTrue(cut.isClosed());
		assertFalse(cut.isClosed());

		verifyAll();
	}

}
