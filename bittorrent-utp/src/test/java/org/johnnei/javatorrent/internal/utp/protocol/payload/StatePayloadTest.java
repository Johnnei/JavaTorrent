package org.johnnei.javatorrent.internal.utp.protocol.payload;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests {@link StatePayload}
 */
public class StatePayloadTest {

	@Test
	public void testGetType() {
		assertEquals(2, new StatePayload().getType());
	}

	@Test
	public void testProcess() {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetMock = mock(UtpPacket.class);

		StatePayload cut = new StatePayload();
		cut.process(packetMock, socketMock);

		verifyNoMoreInteractions(socketMock, packetMock);
	}

	@Test
	public void testToString() {
		assertTrue("Incorrect toString start", new StatePayload().toString().startsWith("StatePayload["));
	}

}