package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.payload.StatePayload;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpAckHandler}
 */
public class UtpAckHandlerTest {

	@Test
	public void testRegisterPackets() {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetOne = mock(UtpPacket.class);
		UtpPacket packetTwo = mock(UtpPacket.class);

		when(packetOne.getPacketSize()).thenReturn(5);
		when(packetTwo.getPacketSize()).thenReturn(7);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		assertEquals("Initial bytes in flight must be 0", 0, cut.countBytesInFlight());

		cut.registerPacket(packetOne);
		assertEquals("First packet size is 5, sum should be only this packet", 5, cut.countBytesInFlight());

		cut.registerPacket(packetTwo);
		assertEquals("Second packet size is 7, sum should be both packets", 12, cut.countBytesInFlight());
	}

	@Test
	public void testOnReset() {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetOne = mock(UtpPacket.class);
		UtpPacket packetTwo = mock(UtpPacket.class);

		when(packetOne.getPacketSize()).thenReturn(5);
		when(packetTwo.getPacketSize()).thenReturn(7);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		cut.registerPacket(packetOne);
		cut.registerPacket(packetTwo);

		assertEquals("Bytes in flight sum should be both packets", 12, cut.countBytesInFlight());

		cut.onReset();

		assertEquals("All packets in flight should be dropped after a reset", 0, cut.countBytesInFlight());
	}

	@Test
	public void testReceiveMisorderedPackets() throws IOException {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetOne = mock(UtpPacket.class);
		UtpPacket packetTwo = mock(UtpPacket.class);

		when(packetOne.getSequenceNumber()).thenReturn((short) 1);
		when(packetOne.getPacketSize()).thenReturn(5);
		when(packetTwo.getSequenceNumber()).thenReturn((short) 2);
		when(packetTwo.getPacketSize()).thenReturn(7);

		// Packet to initialize the ACK numbers.
		UtpPacket initPacket = mock(UtpPacket.class);
		when(initPacket.getSequenceNumber()).thenReturn((short) 5);

		UtpPacket ackOne = mock(UtpPacket.class);
		UtpPacket ackTwo = mock(UtpPacket.class);

		when(ackOne.getSequenceNumber()).thenReturn((short) 6);
		when(ackOne.getAcknowledgeNumber()).thenReturn((short) 1);
		when(ackTwo.getSequenceNumber()).thenReturn((short) 7);
		when(ackTwo.getAcknowledgeNumber()).thenReturn((short) 2);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		cut.registerPacket(packetOne);
		cut.registerPacket(packetTwo);

		assertEquals("Bytes in flight before acks is incorrect", 12, cut.countBytesInFlight());

		cut.onReceivedPacket(initPacket);
		assertEquals("After init packet the return ACK number must be 5.", 5, cut.getAcknowledgeNumber());
		// Initial packet must not cause a ST_STATE as it will be received during the SYN-phase.
		verify(socketMock, never()).send(isA(StatePayload.class));

		cut.onReceivedPacket(ackTwo);

		assertEquals("After first ack the second packet should no longer be in flight.", 5, cut.countBytesInFlight());
		assertEquals("After first ack the return ACK number must be 5 (Packet 6 is not acked yet).", 5, cut.getAcknowledgeNumber());
		// Even though a packet got ack'ed we don't know about packet 6 yet, so we MUST NOT send out a 7 (which also confirms 6).
		verify(socketMock, never()).send(isA(StatePayload.class));

		cut.onReceivedPacket(ackOne);

		assertEquals("Bytes in flight after acks is incorrect", 0, cut.countBytesInFlight());
		assertEquals("After second ack the return ACK number must be 7.", 7, cut.getAcknowledgeNumber());
		verify(socketMock, times(2)).send(isA(StatePayload.class));
	}

}