package org.johnnei.javatorrent.internal.utp.stream;

import java.io.IOException;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class UtpInputStreamTest {

	private UtpInputStream cut = new UtpInputStream((short) 1);

	@Test
	public void testReadReceivedInOrder() throws IOException {
		cut.submitData((short) 1, new byte[]{ 5, 6, 7, 8 });
		cut.submitData((short) 2, new byte[]{ 1, 2, 3, 4 });

		assertThat(cut.read(), equalTo(5));
		assertThat(cut.read(), equalTo(6));
		assertThat(cut.read(), equalTo(7));
		assertThat(cut.read(), equalTo(8));
		assertThat(cut.read(), equalTo(1));
		assertThat(cut.read(), equalTo(2));
		assertThat(cut.read(), equalTo(3));
		assertThat(cut.read(), equalTo(4));
	}

	@Test
	public void testReadReceivedOutOfOrder() throws IOException {
		cut.submitData((short) 2, new byte[]{ 1, 2, 3, 4 });
		cut.submitData((short) 1, new byte[]{ 5, 6, 7, 8 });

		assertThat(cut.read(), equalTo(5));
		assertThat(cut.read(), equalTo(6));
		assertThat(cut.read(), equalTo(7));
		assertThat(cut.read(), equalTo(8));
		assertThat(cut.read(), equalTo(1));
		assertThat(cut.read(), equalTo(2));
		assertThat(cut.read(), equalTo(3));
		assertThat(cut.read(), equalTo(4));
	}

	@Test
	public void testReadReceivedOutOfOrderBadly() throws IOException {
		cut.submitData((short) 5, new byte[]{ 4 });
		cut.submitData((short) 4, new byte[]{ 3 });
		cut.submitData((short) 3, new byte[]{ 2 });
		cut.submitData((short) 2, new byte[]{ 1 });
		cut.submitData((short) 1, new byte[]{ 5, 6, 7, 8 });

		assertThat(cut.read(), equalTo(5));
		assertThat(cut.read(), equalTo(6));
		assertThat(cut.read(), equalTo(7));
		assertThat(cut.read(), equalTo(8));
		assertThat(cut.read(), equalTo(1));
		assertThat(cut.read(), equalTo(2));
		assertThat(cut.read(), equalTo(3));
		assertThat(cut.read(), equalTo(4));
	}

	@Test
	public void testAvailable() {
		assertThat("No packet are available.", cut.available(), equalTo(0));
		cut.submitData((short) 1, new byte[]{ 5, 6, 7, 8 });
		assertThat("Packet is available.", cut.available(), equalTo(4));
		cut.submitData((short) 3, new byte[]{ 2 });
		assertThat("Packet received is out of order.", cut.available(), equalTo(4));
		cut.submitData((short) 2, new byte[]{ 1 });
		assertThat("Packet order has been restored.", cut.available(), equalTo(6));
	}
}
