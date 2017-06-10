package org.johnnei.javatorrent.internal.utp;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PacketAckHandlerTest {

	private UtpSocket socket;

	private PacketAckHandler cut;

	@Before
	public void setUp() {
		socket = mock(UtpSocket.class);
		cut = new PacketAckHandler(socket, (short) 1);
	}

	@Test
	public void testOnReceivedPacket() {
		UtpPacket packet = mock(UtpPacket.class);
		UtpHeader header = mock(UtpHeader.class);
		when(packet.getHeader()).thenReturn(header);
		when(header.getSequenceNumber()).thenReturn((short) 2);

		cut.onReceivedPacket(packet);

		ArgumentCaptor<Acknowledgement> acknowledgementCaptor = ArgumentCaptor.forClass(Acknowledgement.class);
		verify(socket).acknowledgePacket(acknowledgementCaptor.capture());
		assertThat("Ack should be sent of for the second packet.", acknowledgementCaptor.getValue().getSequenceNumber(), equalTo((short) 2));
		assertThat("This is the first occurrence of the packet.", acknowledgementCaptor.getValue().getTimesSent(), equalTo(1));
	}

	@Test
	public void testOnReceivedPacketOutOfOrder() {
		UtpPacket packetTwo = mock(UtpPacket.class);
		UtpHeader headerTwo = mock(UtpHeader.class);
		when(packetTwo.getHeader()).thenReturn(headerTwo);
		when(headerTwo.getSequenceNumber()).thenReturn((short) 2);

		UtpPacket packetThree = mock(UtpPacket.class);
		UtpHeader headerThree = mock(UtpHeader.class);
		when(packetThree.getHeader()).thenReturn(headerThree);
		when(headerThree.getSequenceNumber()).thenReturn((short) 3);

		cut.onReceivedPacket(packetThree);

		// Packet two should be acked first.
		verify(socket, never()).acknowledgePacket(any(Acknowledgement.class));

		cut.onReceivedPacket(packetTwo);

		ArgumentCaptor<Acknowledgement> acknowledgementCaptor = ArgumentCaptor.forClass(Acknowledgement.class);

		// Both packets should be acked at once now.
		verify(socket, times(2)).acknowledgePacket(acknowledgementCaptor.capture());

		List<Acknowledgement> acknowledgements = acknowledgementCaptor.getAllValues();

		assertThat("Ack should be sent in order of sequence.", acknowledgements.get(0).getSequenceNumber(), equalTo((short) 2));
		assertThat("This is the first occurrence of the packet.", acknowledgements.get(0).getTimesSent(), equalTo(1));

		assertThat("Ack should be sent in order of sequence.", acknowledgements.get(1).getSequenceNumber(), equalTo((short) 3));
		assertThat("This is the first occurrence of the packet.", acknowledgements.get(1).getTimesSent(), equalTo(1));
	}

	@Test
	public void testOnReceivedPacketUninitializedSocket() {
		UtpPacket packetTwo = mock(UtpPacket.class);
		UtpHeader headerTwo = mock(UtpHeader.class);
		when(packetTwo.getHeader()).thenReturn(headerTwo);
		when(headerTwo.getSequenceNumber()).thenReturn((short) 2);

		UtpPacket packetThree = mock(UtpPacket.class);
		UtpHeader headerThree = mock(UtpHeader.class);
		when(packetThree.getHeader()).thenReturn(headerThree);
		when(headerThree.getSequenceNumber()).thenReturn((short) 3);

		cut = new PacketAckHandler(socket);
		cut.onReceivedPacket(packetTwo);

		// Packet two should not be explicitly acked as it will be acked by the connection handshake system.
		verify(socket, never()).acknowledgePacket(any(Acknowledgement.class));
		verify(socket).setAcknowledgeNumber((short) 2);

		cut.onReceivedPacket(packetThree);

		ArgumentCaptor<Acknowledgement> acknowledgementCaptor = ArgumentCaptor.forClass(Acknowledgement.class);

		// Only packet 3 should be explicitly acked.
		verify(socket).acknowledgePacket(acknowledgementCaptor.capture());

		Acknowledgement acknowledgement = acknowledgementCaptor.getValue();
		assertThat("Ack should be sent in order of sequence.", acknowledgement.getSequenceNumber(), equalTo((short) 3));
		assertThat("This is the first occurrence of the packet.", acknowledgement.getTimesSent(), equalTo(1));
	}

}
