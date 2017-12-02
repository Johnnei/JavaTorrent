package org.johnnei.javatorrent.internal.utp;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PacketSizeHandlerTest {

	private SocketWindowHandler windowHandler;

	private PacketSizeHandler cut;

	private UtpPacket sentPacket;
	private UtpPacket receivedPacket;

	@BeforeEach
	public void setUp() {
		windowHandler = mock(SocketWindowHandler.class);
		cut = new PacketSizeHandler(windowHandler);

		sentPacket = mock(UtpPacket.class);
		UtpHeader sentHeader = mock(UtpHeader.class);

		when(sentPacket.getHeader()).thenReturn(sentHeader);
		when(sentHeader.getSequenceNumber()).thenReturn((short) 5);

		receivedPacket = mock(UtpPacket.class);
		UtpHeader receivedHeader = mock(UtpHeader.class);

		when(receivedPacket.getHeader()).thenReturn(receivedHeader);
		when(receivedHeader.getAcknowledgeNumber()).thenReturn((short) 5);
	}

	@Test
	public void testIgnoreDuplicateAck() {
		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getPacketSize(), is(150));
	}

	@Test
	public void testInitialSize() {
		assertThat(cut.getPacketSize(), is(150));
	}

	@Test
	public void testScaleSizeUp() {
		when(windowHandler.getMaxWindow()).thenReturn(3000);

		cut.onSentPacket(sentPacket);
		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getPacketSize(), is(300));
	}

	@Test
	public void testScaleSizeDown() {
		when(windowHandler.getMaxWindow()).thenReturn(3000).thenReturn(1500);

		cut.onSentPacket(sentPacket);
		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getPacketSize(), is(300));

		cut.onSentPacket(sentPacket);
		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getPacketSize(), is(150));
	}

	@Test
	public void testPacketLossDownScale() {
		when(windowHandler.getMaxWindow()).thenReturn(3000);

		cut.onSentPacket(sentPacket);
		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getPacketSize(), is(300));

		cut.onPacketLoss();
		cut.onSentPacket(sentPacket);
		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getPacketSize(), is(157));
	}

	@Test
	public void testTimeout() {
		when(windowHandler.getMaxWindow()).thenReturn(3000);

		cut.onSentPacket(sentPacket);
		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getPacketSize(), not(is(150)));

		cut.onTimeout();

		assertThat(cut.getPacketSize(), is(150));
	}

}
