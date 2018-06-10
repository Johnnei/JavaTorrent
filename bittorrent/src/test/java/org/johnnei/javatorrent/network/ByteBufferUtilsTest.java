package org.johnnei.javatorrent.network;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ByteBufferUtilsTest {

	@Test
	public void testPutString() {
		ByteBuffer buffer = ByteBuffer.allocate(32);

		ByteBufferUtils.putString(buffer, "BitTorrent protocol");

		byte[] expectedOutput = new byte[] {
			0x42, 0x69, 0x74, 0x54, 0x6F, 0x72, 0x72, 0x65, 0x6E, 0x74, 0x20,
			0x70, 0x72, 0x6F, 0x74, 0x6F, 0x63, 0x6F, 0x6C
		};

		buffer.flip();
		byte[] output = new byte[expectedOutput.length];
		buffer.get(output);
		assertThat(output, equalTo(expectedOutput));
	}
}
