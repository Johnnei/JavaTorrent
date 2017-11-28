package org.johnnei.javatorrent.internal.utp.protocol.packet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.internal.utp.protocol.PacketType;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UtpPacketTest {

	private UtpPacket cut;

	@BeforeEach
	public void setUp() {
		UtpHeader header = mock(UtpHeader.class);
		Payload payload = mock(Payload.class);

		when(header.getType()).thenReturn((byte) 0);
		when(payload.getType()).thenReturn(PacketType.DATA);

		cut = new UtpPacket(header, payload);
	}

	@Test
	public void testIncrementSentCountIsZeroOnCreation() throws Exception {
		assertThat(cut.isSendOnce(), is(false));
	}

	@Test
	public void testIncrementSentCount() throws Exception {
		cut.incrementSentCount();
		assertThat(cut.isSendOnce(), is(true));
	}

	@Test
	public void testIncrementSentCountTwice() throws Exception {
		cut.incrementSentCount();
		cut.incrementSentCount();
		assertThat(cut.isSendOnce(), is(false));
	}

}
