package org.johnnei.javatorrent.internal.utp.stream;

import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class InputPacketSorterTest {

	private Pipe pipe;

	private InputPacketSorter cut;

	@BeforeEach
	public void setUp() throws Exception {
		pipe = Pipe.open();
		pipe.sink().configureBlocking(false);
		pipe.source().configureBlocking(false);
		cut = new InputPacketSorter(pipe.sink(), (short) 1);
	}

	@AfterEach
	public void tearDown() throws Exception {
		pipe.source().close();
		pipe.sink().close();
	}

	@Test
	public void testReadBuffered() throws Exception {
		cut.submitData((short) 1, new byte[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 });

		ByteBuffer buffer = ByteBuffer.allocate(5);

		assertThat(pipe.source().read(buffer), is(5));
		assertThat(buffer.array(), equalTo(new byte[] { 1, 2, 3, 4, 5 }));
		buffer.flip();
		assertThat(pipe.source().read(buffer), is(5));
		assertThat(buffer.array(), equalTo(new byte[] { 6, 7, 8, 9, 10 }));
	}

	@Test
	public void testReadBufferedMultiplePackets() throws Exception {
		cut.submitData((short) 1, new byte[] { 1, 2, 3, 4, 5 });
		cut.submitData((short) 2, new byte[] { 6, 7, 8, 9, 10 });
		ByteBuffer buffer = ByteBuffer.allocate(7);
		assertThat(pipe.source().read(buffer), equalTo(7));
		assertThat(buffer.array(), equalTo(new byte[] { 1, 2, 3, 4, 5, 6, 7 }));
		buffer.flip();
		assertThat(pipe.source().read(buffer), equalTo(3));
		assertThat(buffer.array(), equalTo(new byte[] { 8, 9, 10, 4, 5, 6, 7 }));
	}

	@Test
	public void testReadReceivedInOrder() throws Exception {
		cut.submitData((short) 1, new byte[]{ 5, 6, 7, 8 });
		cut.submitData((short) 2, new byte[]{ 1, 2, 3, 4 });

		ByteBuffer buffer = ByteBuffer.allocate(8);
		assertThat(pipe.source().read(buffer), is(8));
		buffer.flip();

		assertThat(buffer.get(), equalTo((byte) 5));
		assertThat(buffer.get(), equalTo((byte) 6));
		assertThat(buffer.get(), equalTo((byte) 7));
		assertThat(buffer.get(), equalTo((byte) 8));
		assertThat(buffer.get(), equalTo((byte) 1));
		assertThat(buffer.get(), equalTo((byte) 2));
		assertThat(buffer.get(), equalTo((byte) 3));
		assertThat(buffer.get(), equalTo((byte) 4));
	}

	@Test
	public void testReadReceivedOutOfOrder() throws Exception {
		cut.submitData((short) 2, new byte[]{ 1, 2, 3, 4 });
		cut.submitData((short) 1, new byte[]{ 5, 6, 7, 8 });

		ByteBuffer buffer = ByteBuffer.allocate(8);
		assertThat(pipe.source().read(buffer), is(8));
		buffer.flip();

		assertThat(buffer.get(), equalTo((byte) 5));
		assertThat(buffer.get(), equalTo((byte) 6));
		assertThat(buffer.get(), equalTo((byte) 7));
		assertThat(buffer.get(), equalTo((byte) 8));
		assertThat(buffer.get(), equalTo((byte) 1));
		assertThat(buffer.get(), equalTo((byte) 2));
		assertThat(buffer.get(), equalTo((byte) 3));
		assertThat(buffer.get(), equalTo((byte) 4));
	}

	@Test
	public void testReadReceivedOutOfOrderBadly() throws Exception {
		cut.submitData((short) 5, new byte[]{ 4 });
		cut.submitData((short) 4, new byte[]{ 3 });
		cut.submitData((short) 3, new byte[]{ 2 });
		cut.submitData((short) 2, new byte[]{ 1 });
		cut.submitData((short) 1, new byte[]{ 5, 6, 7, 8 });

		ByteBuffer buffer = ByteBuffer.allocate(8);
		assertThat(pipe.source().read(buffer), is(8));
		buffer.flip();

		assertThat(buffer.get(), equalTo((byte) 5));
		assertThat(buffer.get(), equalTo((byte) 6));
		assertThat(buffer.get(), equalTo((byte) 7));
		assertThat(buffer.get(), equalTo((byte) 8));
		assertThat(buffer.get(), equalTo((byte) 1));
		assertThat(buffer.get(), equalTo((byte) 2));
		assertThat(buffer.get(), equalTo((byte) 3));
		assertThat(buffer.get(), equalTo((byte) 4));
	}
}
