package org.johnnei.javatorrent.internal.utp;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.internal.utp.protocol.PacketType;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SocketWindowHandlerTest {

	private SocketWindowHandler cut;

	private UtpPacket sentPacket;
	private UtpPacket receivedPacket;
	private UtpHeader receivedHeader;
	private UtpHeader sentHeader;

	@BeforeEach
	public void setUp() {
		cut = new SocketWindowHandler();

		sentPacket = mock(UtpPacket.class);
		sentHeader = mock(UtpHeader.class);

		when(sentPacket.getHeader()).thenReturn(sentHeader);
		when(sentPacket.getSize()).thenReturn(25);
		when(sentHeader.getSequenceNumber()).thenReturn((short) 42);
		when(sentHeader.getType()).thenReturn(PacketType.DATA.getTypeField());

		receivedPacket = mock(UtpPacket.class);
		receivedHeader = mock(UtpHeader.class);

		when(receivedPacket.getHeader()).thenReturn(receivedHeader);
		when(receivedHeader.getAcknowledgeNumber()).thenReturn((short) 42);
	}

	@Test
	public void testCountBytesInFlightOnlyCountData() {
		when(sentHeader.getType()).thenReturn(PacketType.STATE.getTypeField());
		cut.onSentPacket(sentPacket);

		assertThat(cut.getBytesInFlight(), is(0));
	}

	@Test
	public void testCountBytesInFlight() {
		cut.onSentPacket(sentPacket);

		assertThat(cut.getBytesInFlight(), is(25));
	}

	@Test
	public void testOnReceivedPacketReturnsAckedPacked() {
		cut.onSentPacket(sentPacket);
		Optional<UtpPacket> ackedPacket = cut.onReceivedPacket(receivedPacket);
		assertThat(ackedPacket.orElse(null), is(sentPacket));
	}

	@Test
	public void testCountBytesInFlightAckedPacket() {
		cut.onSentPacket(sentPacket);
		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getBytesInFlight(), is(0));
	}

	@Test
	public void testOnTimeout() {
		// Cause the window to drift.
		cut.onSentPacket(sentPacket);

		when(receivedHeader.getTimestampDifference())
			.thenReturn(75_521)
			.thenReturn(78_344);
		cut.onReceivedPacket(receivedPacket);
		cut.onSentPacket(sentPacket);
		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getMaxWindow(), not(is(150)));

		cut.onTimeout();

		assertThat(cut.getMaxWindow(), is(150));
	}

	@Test
	public void testOnPacketLoss() {
		UtpPacket packet = mock(UtpPacket.class);
		when(packet.getHeader()).thenReturn(mock(UtpHeader.class));
		cut.onPacketLoss(packet);
		assertThat(cut.getMaxWindow(), is(75));
	}

	@Test
	public void testIncreaseWindowSizeOnFastSocket() {
		cut.onSentPacket(sentPacket);

		when(receivedHeader.getTimestampDifference())
			.thenReturn(75_521)
			.thenReturn(78_344);

		cut.onReceivedPacket(receivedPacket);

		assertThat("The first packet can't have a delay", cut.getMaxWindow(), is(150));

		cut.onSentPacket(sentPacket);

		// our_delay = 2'823
		// target = 100'000
		// off_target = 97'177
		// outstanding_packet = 25
		// delay_factor = 0.97177
		// window_factor = 0.16666
		// scaled gain -> 500 * 0.97177 * 0.15822 = 80.97 -> 80

		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getMaxWindow(), is(230));
	}

	@Test
	public void testIncreaseWindowSizeOnSlowSocket() {
		cut.onSentPacket(sentPacket);

		when(receivedHeader.getTimestampDifference())
			.thenReturn(75_521)
			.thenReturn(253_486);

		// our_delay = 0 (the first packet can't have delay)

		cut.onReceivedPacket(receivedPacket);

		assertThat("The first packet can't have a delay", cut.getMaxWindow(), is(150));

		cut.onSentPacket(sentPacket);

		// our_delay = 177'965
		// target = 100'000
		// off_target = -77'965
		// outstanding_packet = 25
		// delay_factor = -0.77965
		// window_factor = 0.16666
		// scaled gain -> 500 * -0.77965 * 0.16666 = -64.94 -> -64

		cut.onReceivedPacket(receivedPacket);

		assertThat(cut.getMaxWindow(), is(86));
	}

}
