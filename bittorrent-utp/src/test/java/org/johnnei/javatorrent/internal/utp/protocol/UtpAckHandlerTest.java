package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;
import java.util.List;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.payload.StatePayload;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Matchers.same;
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

		when(packetOne.getSequenceNumber()).thenReturn((short) 1);
		when(packetOne.getPacketSize()).thenReturn(5);
		when(packetTwo.getSequenceNumber()).thenReturn((short) 2);
		when(packetTwo.getPacketSize()).thenReturn(7);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		assertEquals("Initial bytes in flight must be 0", 0, cut.countBytesInFlight());

		cut.registerPacket(packetOne);
		assertEquals("First packet size is 5, sum should be only this packet", 5, cut.countBytesInFlight());

		cut.registerPacket(packetTwo);
		assertEquals("Second packet size is 7, sum should be both packets", 12, cut.countBytesInFlight());
	}

	@Test
	public void testRegisterPacketIgnoreResend() {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetOne = mock(UtpPacket.class);

		when(packetOne.getSequenceNumber()).thenReturn((short) 1);
		when(packetOne.getPacketSize()).thenReturn(5);
		when(packetOne.getTimesSent()).thenReturn(1);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		assertEquals("Initial bytes in flight must be 0", 0, cut.countBytesInFlight());

		cut.registerPacket(packetOne);
		assertEquals("Packet should have been ignored.", 0, cut.countBytesInFlight());
	}

	@Test
	public void testRegisterPacketIgnoreState() {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetOne = mock(UtpPacket.class);

		when(packetOne.getSequenceNumber()).thenReturn((short) 1);
		when(packetOne.getPacketSize()).thenReturn(5);
		when(packetOne.getType()).thenReturn((byte) UtpProtocol.ST_STATE);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		assertEquals("Initial bytes in flight must be 0", 0, cut.countBytesInFlight());

		cut.registerPacket(packetOne);
		assertEquals("Packet should have been ignored.", 0, cut.countBytesInFlight());
	}

	@Test
	public void testRegisterPacketsIgnoreDuplicates() {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetOne = mock(UtpPacket.class);

		when(packetOne.getSequenceNumber()).thenReturn((short) 1);
		when(packetOne.getPacketSize()).thenReturn(5);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		assertEquals("Initial bytes in flight must be 0", 0, cut.countBytesInFlight());

		cut.registerPacket(packetOne);
		assertEquals("First packet size is 5, sum should be only this packet", 5, cut.countBytesInFlight());

		cut.registerPacket(packetOne);
		assertEquals("Duplicate packet size is 5, sum should be only the first packet.", 5, cut.countBytesInFlight());
	}

	@Test
	public void testOnReset() {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetOne = mock(UtpPacket.class);
		UtpPacket packetTwo = mock(UtpPacket.class);

		when(packetOne.getSequenceNumber()).thenReturn((short) 1);
		when(packetOne.getPacketSize()).thenReturn(5);
		when(packetTwo.getSequenceNumber()).thenReturn((short) 2);
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
		when(initPacket.getAcknowledgeNumber()).thenReturn((short) 0);

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

		List<UtpPacket> ackedPackets = cut.onReceivedPacket(initPacket);
		assertEquals("After init packet the return ACK number must be 5.", 5, cut.getAcknowledgeNumber());
		assertThat("No packets were acked by the init packet", ackedPackets, empty());
		// Initial packet must not cause a ST_STATE as it will be received during the SYN-phase.
		verify(socketMock, never()).sendUnbounded(isA(UtpPacket.class));

		ackedPackets = cut.onReceivedPacket(ackTwo);

		assertEquals("After first ack the second packet should also be no longer be in flight, the ACK packet might have been lost.",
				0, cut.countBytesInFlight());
		assertEquals("After first ack the return ACK number must be 5 (Packet 6 is not acked yet).", 5, cut.getAcknowledgeNumber());
		assertThat("All packets should have been returned as ACK'ed.", ackedPackets, Matchers.contains(packetOne, packetTwo));
		// Even though a packet got ack'ed we don't know about packet 6 yet, so we MUST NOT send out a 7 (which also confirms 6).
		verify(socketMock, never()).sendUnbounded(isA(UtpPacket.class));

		cut.onReceivedPacket(ackOne);

		assertEquals("Bytes in flight after acks is incorrect", 0, cut.countBytesInFlight());
		assertEquals("After second ack the return ACK number must be 7.", 7, cut.getAcknowledgeNumber());
		verify(socketMock, times(2)).sendUnbounded(isA(UtpPacket.class));
	}

	@Test
	public void testDoNotAckOnStatePackets() throws IOException {
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

		// ST_STATE with higher seq_nr but also ACK one of our packets.
		when(ackTwo.getSequenceNumber()).thenReturn((short) 7);
		when(ackTwo.getAcknowledgeNumber()).thenReturn((short) 2);
		when(ackTwo.getType()).thenReturn((byte) UtpProtocol.ST_STATE);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		cut.registerPacket(packetOne);
		cut.registerPacket(packetTwo);

		assertEquals("Bytes in flight before acks is incorrect", 12, cut.countBytesInFlight());

		cut.onReceivedPacket(initPacket);
		assertEquals("After init packet the return ACK number must be 5.", 5, cut.getAcknowledgeNumber());
		// Initial packet must not cause a ST_STATE as it will be received during the SYN-phase.
		verify(socketMock, never()).sendUnbounded(isA(UtpPacket.class));

		cut.onReceivedPacket(ackOne);

		assertEquals("Bytes in flight after first ack is incorrect", 7, cut.countBytesInFlight());
		assertEquals("After first ack the return ACK number is incorrect.", 6, cut.getAcknowledgeNumber());

		verify(socketMock, times(1)).sendUnbounded(isA(UtpPacket.class));


		cut.onReceivedPacket(ackTwo);

		assertEquals("Bytes in flight after second ack is incorrect", 0, cut.countBytesInFlight());
		assertEquals("After second ack the return ACK number is incorrect.", 6, cut.getAcknowledgeNumber());

		// Don't send out another ST_STATE to confirm that we received seq_nr 7 as it is not the data packet.
		verify(socketMock, times(1)).sendUnbounded(isA(UtpPacket.class));
	}

	@Test
	public void testResendLostPacketNoLostPacket() throws IOException {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetOne = mock(UtpPacket.class);

		when(packetOne.getSequenceNumber()).thenReturn((short) 1);
		when(packetOne.getPacketSize()).thenReturn(5);

		// Packet to initialize the ACK numbers.
		UtpPacket initPacket = mock(UtpPacket.class);
		when(initPacket.getSequenceNumber()).thenReturn((short) 5);

		UtpPacket ackOne = mock(UtpPacket.class);

		when(ackOne.getSequenceNumber()).thenReturn((short) 5);
		when(ackOne.getAcknowledgeNumber()).thenReturn((short) 1);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		cut.registerPacket(packetOne);

		assertEquals("Bytes in flight before acks is incorrect", 5, cut.countBytesInFlight());

		cut.onReceivedPacket(initPacket);
		assertEquals("After init packet the return ACK number must be 5.", 5, cut.getAcknowledgeNumber());
		// Initial packet must not cause a ST_STATE as it will be received during the SYN-phase.
		verify(socketMock, never()).send(isA(StatePayload.class));

		cut.onReceivedPacket(ackOne);

		assertEquals("After first ack the second packet should no longer be in flight.", 0, cut.countBytesInFlight());

		// Packet must not have been resend yet
		verify(socketMock, never()).sendUnbounded(any());

		for (int i = 0; i < 2; i++) {
			cut.onReceivedPacket(ackOne);
			// Packet must not have been resend as there is no missing packet.
			verify(socketMock, never()).sendUnbounded(any());
		}
	}

	@Test
	public void testResendLostPacket() throws IOException {
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

		when(ackOne.getSequenceNumber()).thenReturn((short) 6);
		when(ackOne.getAcknowledgeNumber()).thenReturn((short) 1);

		UtpAckHandler cut = new UtpAckHandler(socketMock);
		cut.registerPacket(packetOne);

		assertEquals("Bytes in flight before acks is incorrect", 5, cut.countBytesInFlight());

		cut.onReceivedPacket(initPacket);
		assertEquals("After init packet the return ACK number must be 5.", 5, cut.getAcknowledgeNumber());
		// Initial packet must not cause a ST_STATE as it will be received during the SYN-phase.
		verify(socketMock, never()).send(isA(StatePayload.class));

		cut.onReceivedPacket(ackOne);

		assertEquals("After first ack the second packet should no longer be in flight.", 0, cut.countBytesInFlight());

		cut.registerPacket(packetTwo);

		assertEquals("Bytes in flight must have increased after second register", 7, cut.countBytesInFlight());

		// Packet must not have been resend yet
		verify(socketMock, never()).sendUnbounded(same(packetTwo));

		cut.onReceivedPacket(ackOne);
		// Packet must not have been resend yet
		verify(socketMock, never()).sendUnbounded(same(packetTwo));

		// This ack should cause the resend.
		cut.onReceivedPacket(ackOne);
		verify(socketMock, times(1)).sendUnbounded(same(packetTwo));

		assertEquals("Bytes in flight must not change by resending a lost packet.", 7, cut.countBytesInFlight());
	}

}