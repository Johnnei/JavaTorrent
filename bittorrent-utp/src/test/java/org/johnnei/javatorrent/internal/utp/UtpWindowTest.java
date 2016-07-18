package org.johnnei.javatorrent.internal.utp;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpWindow}
 */
public class UtpWindowTest {

	private UtpSocketImpl socketMock;

	private UtpWindow cut;

	@Before
	public void setUp() {
		socketMock = mock(UtpSocketImpl.class);
		cut = new UtpWindow(socketMock);
	}

	@Test
	public void testConstructor() {
		assertEquals("Initial window size must be 150 according to specs", 150, cut.getSize());
	}

	@Test
	public void testUpdate() throws Exception {
		when(socketMock.getBytesInFlight()).thenReturn(88);

		UtpPacket packetMock = mock(UtpPacket.class);
		when(packetMock.getTimestampDifferenceMicroseconds()).thenReturn(52);

		cut.update(packetMock);

		assertEquals("Window size should have increased by 58.", 208, cut.getSize());
	}

	@Test
	public void testOnTimeout() throws Exception {
		cut.onTimeout();

		assertThat("Window size will be set to 150 is it dropped below that.", cut.getSize(), Matchers.greaterThanOrEqualTo(150));
	}

	@Test
	public void testOnTimeoutWindowTooSmall() {
		Whitebox.setInternalState(cut, "maxWindow", 50);
		assertEquals("Failed to modify internal state.", 50, cut.getSize());

		cut.onTimeout();
		assertEquals("Timeout failed to set the window to 150 to allow sending of packets again.", 150, cut.getSize());
	}
}
