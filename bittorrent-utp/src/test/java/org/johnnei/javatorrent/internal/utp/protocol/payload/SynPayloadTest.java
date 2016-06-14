package org.johnnei.javatorrent.internal.utp.protocol.payload;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.ConnectionState;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SynPayload}
 */
public class SynPayloadTest {

	@Test
	public void testGetType() {
		assertEquals(4, new SynPayload().getType());
	}

	@Test
	public void testProcessIgnoreOnDisconnected() throws IOException {
		testIgnorePacket(ConnectionState.CLOSED);
	}

	@Test
	public void testProcessIgnoreOnDisconnecting() throws IOException {
		testIgnorePacket(ConnectionState.DISCONNECTING);
	}

	@Test
	public void testProcessIgnoreOnConnected() throws IOException {
		testIgnorePacket(ConnectionState.CONNECTED);
	}

	private void testIgnorePacket(ConnectionState connectionState) throws IOException {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetMock = mock(UtpPacket.class);

		when(socketMock.getConnectionState()).thenReturn(connectionState);

		SynPayload cut = new SynPayload();
		cut.process(packetMock, socketMock);

		verify(socketMock).getConnectionState();
		verifyNoMoreInteractions(socketMock, packetMock);
	}

	@Test
	public void testProcess() throws Exception {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetMock = mock(UtpPacket.class);

		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTING);
		when(socketMock.getAcknowledgeNumber()).thenReturn((short) 0);
		when(packetMock.getSequenceNumber()).thenReturn((short) 3);

		SynPayload cut = new SynPayload();
		cut.process(packetMock, socketMock);

		verify(socketMock).sendUnbounded(isA(UtpPacket.class));
	}

	@Test
	public void testToString() {
		assertTrue("Incorrect toString start", new SynPayload().toString().startsWith("SynPayload["));
	}

}