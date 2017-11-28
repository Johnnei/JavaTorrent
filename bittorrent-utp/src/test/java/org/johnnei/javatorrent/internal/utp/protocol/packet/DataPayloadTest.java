package org.johnnei.javatorrent.internal.utp.protocol.packet;

import java.nio.ByteBuffer;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class DataPayloadTest {

	@Test
	public void testGetData() {
		ByteBuffer buffer = ByteBuffer.wrap(new byte[] { 1, 2, 3, 4, 5 });
		// Consume a byte
		buffer.get();

		DataPayload cut = new DataPayload(buffer);
		assertThat("Consumed data should not be copied.", cut.getData(), equalTo(new byte[] { 2, 3, 4, 5 } ));
	}

}
