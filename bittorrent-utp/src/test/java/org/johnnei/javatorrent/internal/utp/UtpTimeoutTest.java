package org.johnnei.javatorrent.internal.utp;

import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpTimeout}
 */
public class UtpTimeoutTest {

	@Test
	public void testConstructor() {
		assertEquals("Initial timeout MUST be 1000ms according to spec", 1000, new UtpTimeout().getDuration().toMillis());
	}

	@Test
	public void testUpdateMinimumTimeout() {
		UtpPacket packetMock = mock(UtpPacket.class);
		when(packetMock.getSentTime()).thenReturn(50_000);

		UtpTimeout cut = new UtpTimeout();
		cut.update(75_000, packetMock);

		assertEquals("Timeout must not drop below 500ms according to spec.", 500, cut.getDuration().toMillis());
	}

	@Test
	public void testUpdateIncreaseTimeout() {
		UtpPacket packetMock = mock(UtpPacket.class);
		when(packetMock.getSentTime()).thenReturn(50_000);

		UtpTimeout cut = new UtpTimeout();
		cut.update(1200_000, packetMock);

		assertEquals("Timeout should have decreased", 1293, cut.getDuration().toMillis());
	}

	@Test
	public void testUpdate() {
		UtpPacket packetMock = mock(UtpPacket.class);
		when(packetMock.getSentTime()).thenReturn(50_000);

		UtpTimeout cut = new UtpTimeout();
		cut.update(700_000, packetMock);

		assertEquals("Timeout should have decreased", 731, cut.getDuration().toMillis());
	}

}