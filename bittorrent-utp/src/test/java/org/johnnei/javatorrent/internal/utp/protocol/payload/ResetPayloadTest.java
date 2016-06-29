package org.johnnei.javatorrent.internal.utp.protocol.payload;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests {@link ResetPayload}
 */
public class ResetPayloadTest {

	@Test
	public void testGetType() {
		assertEquals(3, new ResetPayload().getType());
	}

	@Test
	public void testProcess() throws Exception {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetMock = mock(UtpPacket.class);

		ResetPayload cut = new ResetPayload();
		cut.process(packetMock, socketMock);

		verify(socketMock).onReset();
		verifyNoMoreInteractions(socketMock, packetMock);
	}

	@Test
	public void testToString() {
		assertTrue("Incorrect toString start", new ResetPayload().toString().startsWith("ResetPayload["));
	}

}