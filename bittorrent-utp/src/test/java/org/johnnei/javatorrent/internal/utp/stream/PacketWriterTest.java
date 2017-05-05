package org.johnnei.javatorrent.internal.utp.stream;

import java.nio.ByteBuffer;

import org.junit.Test;

import org.johnnei.javatorrent.internal.utp.protocol.packet.Payload;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpHeader;
import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PacketWriterTest {

	@Test
	public void testWrite() throws Exception {
		PacketWriter cut = new PacketWriter();

		UtpPacket packet = mock(UtpPacket.class);
		UtpHeader header = mock(UtpHeader.class);
		Payload payload = mock(Payload.class);

		when(packet.getPayload()).thenReturn(payload);
		when(packet.getHeader()).thenReturn(header);
		when(payload.getData()).thenReturn(new byte[] { 1, 2, 3, 4, 5});
		when(header.getType()).thenReturn((byte) 3);
		when(header.getVersion()).thenReturn((byte) 1);
		when(header.getConnectionId()).thenReturn((short) 0x1234);
		when(header.getTimestamp()).thenReturn(0x43211234);
		when(header.getTimestampDifference()).thenReturn(0x34122143);
		when(header.getWindowSize()).thenReturn(0x56785687);
		when(header.getSequenceNumber()).thenReturn((short) 0x8756);
		when(header.getAcknowledgeNumber()).thenReturn((short) 0x7865);

		ByteBuffer buffer = cut.write(packet);

		assertThat("20 packet overhead + 5 bytes of payload.", buffer.limit(), equalTo(25));
		assertThat("Bits   0 -   8 should be the type and version", buffer.get(), equalTo((byte) 0x31));
		assertThat("Bits   8 -  16 should be the extension (none supported)", buffer.get(), equalTo((byte) 0));
		assertThat("Bits  16 -  32 should be the connection id", buffer.getShort(), equalTo(header.getConnectionId()));
		assertThat("Bits  32 -  64 should be the timestamp in microseconds", buffer.getInt(), equalTo(header.getTimestamp()));
		assertThat("Bits  64 -  96 should be the timestamp difference in microseconds", buffer.getInt(), equalTo(header.getTimestampDifference()));
		assertThat("Bits  96 - 128 should be the window size", buffer.getInt(), equalTo(header.getWindowSize()));
		assertThat("Bits 128 - 144 should be the sequence number", buffer.getShort(), equalTo(header.getSequenceNumber()));
		assertThat("Bits 144 - 160 should be the acknowledge number", buffer.getShort(), equalTo(header.getAcknowledgeNumber()));
	}

}
