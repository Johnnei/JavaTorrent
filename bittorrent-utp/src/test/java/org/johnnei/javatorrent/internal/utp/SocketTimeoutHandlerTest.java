package org.johnnei.javatorrent.internal.utp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SocketTimeoutHandlerTest {

	@Mock
	private PrecisionTimer precisionTimer;

	@InjectMocks
	private SocketTimeoutHandler cut;

	@Test
	public void testOnReceivedPacket() throws Exception {
		UtpPacket packet = mock(UtpPacket.class);
		UtpHeader header = mock(UtpHeader.class);
		when(packet.getHeader()).thenReturn(header);
		when(packet.isSendOnce()).thenReturn(true);
		when(header.getTimestamp()).thenReturn(525_000);
		when(precisionTimer.getCurrentMicros()).thenReturn(575_000);

		cut.onReceivedPacket(packet);

		// delta = 50'000
		assertThat(cut.getRoundTripTimeVariance(), equalTo(12_500));
		assertThat(cut.getRoundTripTime(), equalTo(6_250));
		// Raised from 56 to the minimum of 500
		assertThat(cut.getTimeout(), equalTo(500));
	}

	@Test
	public void testOnReceivedPacketSlowPacket() throws Exception {
		UtpPacket packet = mock(UtpPacket.class);
		UtpHeader header = mock(UtpHeader.class);
		when(packet.getHeader()).thenReturn(header);
		when(packet.isSendOnce()).thenReturn(true);
		when(header.getTimestamp()).thenReturn(25_000);
		when(precisionTimer.getCurrentMicros()).thenReturn(575_000);

		cut.onReceivedPacket(packet);

		// delta = 550'000
		assertThat(cut.getRoundTripTimeVariance(), equalTo(137_500));
		assertThat(cut.getRoundTripTime(), equalTo(68_750));
		assertThat(cut.getTimeout(), equalTo(618));
	}

	@Test
	public void testOnReceivedPacketVariances() throws Exception {
		UtpPacket packet = mock(UtpPacket.class);
		UtpHeader header = mock(UtpHeader.class);
		when(packet.getHeader()).thenReturn(header);
		when(packet.isSendOnce()).thenReturn(true);
		when(header.getTimestamp()).thenReturn(25_000);

		UtpPacket packet2 = mock(UtpPacket.class);
		UtpHeader header2 = mock(UtpHeader.class);
		when(packet2.getHeader()).thenReturn(header2);
		when(packet2.isSendOnce()).thenReturn(true);
		when(header2.getTimestamp()).thenReturn(600_000);

		when(precisionTimer.getCurrentMicros()).thenReturn(575_000).thenReturn(1_275_000);

		cut.onReceivedPacket(packet);

		// packet_rtt = 550'000
		// delta = 550'000
		assertThat(cut.getRoundTripTimeVariance(), equalTo(137_500));
		assertThat(cut.getRoundTripTime(), equalTo(68_750));
		assertThat(cut.getTimeout(), equalTo(618));

		cut.onReceivedPacket(packet2);

		// packet_rtt = 675'000
		// delta = -606'250
		assertThat(cut.getRoundTripTimeVariance(), equalTo(254_687));
		assertThat(cut.getRoundTripTime(), equalTo(144_531));
		assertThat(cut.getTimeout(), equalTo(1_163));
	}

	@Test
	public void testIgnoreResentPackets() {
		UtpPacket packet = mock(UtpPacket.class);
		when(packet.isSendOnce()).thenReturn(false);
		
		cut.onReceivedPacket(packet);
		assertThat(cut.getTimeout(), equalTo(1_000));
	}

	@Test
	public void testIsTimeoutExpired() {
		when(precisionTimer.getCurrentMicros())
			.thenReturn(1_000_000)
			.thenReturn(1_500_000)
			.thenReturn(2_000_000)
			.thenReturn(2_500_000);

		cut.onSentPacket();

		assertThat("500ms remaining", cut.isTimeoutExpired(), is(false));
		assertThat("0ms remaining", cut.isTimeoutExpired(), is(false));
		assertThat("-500ms remaining", cut.isTimeoutExpired(), is(true));
	}

	@Test
	public void testInitialTimeout() {
		assertThat(cut.getTimeout(), equalTo(1_000));
	}

	@Test
	public void testOnTimeout() {
		cut.onTimeout();
		assertThat(cut.getTimeout(), equalTo(2_000));
	}

}
