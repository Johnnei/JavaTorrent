package org.johnnei.javatorrent.internal.utp.stream;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import org.johnnei.javatorrent.internal.utp.protocol.packet.UtpPacket;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class PacketReaderTest {

	@Test
	public void testRead() throws Exception {
		ByteBuffer buffer = ByteBuffer.allocate(20);
		buffer.put((byte) 0x41);
		buffer.put((byte) 0);
		buffer.putShort((short) 0x1234);
		buffer.putInt(0x43211234);
		buffer.putInt(0x34122143);
		buffer.putInt(0x56785687);
		buffer.putShort((short) 0x8756);
		buffer.putShort((short) 0x7865);
		buffer.flip();

		PacketReader cut = new PacketReader();
		UtpPacket packet = cut.read(buffer);

		assertThat("Bits   0 -   4 should be the type and version", packet.getHeader().getType(), equalTo((byte) 4));
		assertThat("Bits   4 -   8 should be the type and version", packet.getHeader().getVersion(), equalTo((byte) 1));
		assertThat("Bits   8 -  16 should be the extension (none supported)", packet.getHeader().getExtension(), equalTo((byte) 0));
		assertThat("Bits  16 -  32 should be the connection id", packet.getHeader().getConnectionId(), equalTo((short) 0x1234));
		assertThat("Bits  32 -  64 should be the timestamp in microseconds", packet.getHeader().getTimestamp(), equalTo(0x43211234));
		assertThat("Bits  64 -  96 should be the timestamp difference in microseconds", packet.getHeader().getTimestampDifference(), equalTo(0x34122143));
		assertThat("Bits  96 - 128 should be the window size", packet.getHeader().getWindowSize(), equalTo(0x56785687));
		assertThat("Bits 128 - 144 should be the sequence number", packet.getHeader().getSequenceNumber(), equalTo((short) 0x8756));
		assertThat("Bits 144 - 160 should be the acknowledge number", packet.getHeader().getAcknowledgeNumber(), equalTo((short) 0x7865));
	}

}
