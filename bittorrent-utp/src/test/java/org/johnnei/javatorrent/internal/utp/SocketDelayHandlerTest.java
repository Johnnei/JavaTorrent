package org.johnnei.javatorrent.internal.utp;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SocketDelayHandlerTest {

	@Mock
	private PrecisionTimer timerMock;

	@InjectMocks
	private SocketDelayHandler cut;

	@Test
	public void testOnReceivedPacket() throws Exception {
		UtpPacket receivePacket = mock(UtpPacket.class);
		UtpHeader header = mock(UtpHeader.class);

		when(receivePacket.getHeader()).thenReturn(header);
		when(timerMock.getCurrentMicros()).thenReturn(1500);
		when(header.getTimestamp()).thenReturn(5500);

		cut.onReceivedPacket(receivePacket);

		assertThat(cut.getMeasuredDelay(), is(-4000));
	}

	@Test
	public void testGetMeasuredDelay() throws Exception {
		assertThat("Initial delay must be zero.", cut.getMeasuredDelay(), is(0));
	}

}
