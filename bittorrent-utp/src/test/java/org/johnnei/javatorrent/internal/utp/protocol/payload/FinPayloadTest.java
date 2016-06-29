package org.johnnei.javatorrent.internal.utp.protocol.payload;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link FinPayload}
 */
public class FinPayloadTest {

	@Test
	public void testProcess() throws IOException {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetMock = mock(UtpPacket.class);

		when(packetMock.getSequenceNumber()).thenReturn((short) 5);

		FinPayload cut = new FinPayload();
		cut.process(packetMock, socketMock);

		verify(socketMock).setEndOfStreamSequenceNumber(eq((short) 5));
		verify(socketMock).close();
	}

	@Test
	public void testGetType() {
		assertEquals(1, new FinPayload().getType());
	}

	@Test
	public void testToString() {
		assertTrue("Incorrect toString start", new FinPayload().toString().startsWith("FinPayload["));
	}

}