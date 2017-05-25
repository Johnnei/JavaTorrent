package org.johnnei.javatorrent.internal.utp.stream;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import org.johnnei.javatorrent.internal.utp.UtpSocket;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UtpOutputStreamTest {

	@Mock
	private UtpSocket socket;

	@InjectMocks
	private UtpOutputStream cut;

	@Test
	public void testWriteCacheUntilPacketSize() {
		when(socket.getPacketPayloadSize()).thenReturn(10);

		byte[] data = new byte[9];
		cut.write(data);

		verify(socket, never()).send(any());
	}

	@Test
	public void testWriteOverPacketSize() {
		when(socket.getPacketPayloadSize()).thenReturn(10);

		byte[] data = new byte[10];
		cut.write(data);

		verify(socket).send(any());
	}

	@Test
	public void testWriteMergeMultipleWrites() {
		when(socket.getPacketPayloadSize()).thenReturn(10);

		byte[] data1 = new byte[]{5, 3};
		byte[] data2 = new byte[]{12, 25};
		byte[] data3 = new byte[]{54, 12};
		byte[] data4 = new byte[]{13, 49};
		cut.write(data1);
		cut.write(data2);
		cut.write(24);
		cut.write(data3);
		cut.write(42);
		cut.write(data4);

		ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(socket).send(bufferCaptor.capture());

		assertThat(
			"Data over multiple writes should remain in order.",
			bufferCaptor.getValue().array(),
			equalTo(new byte[]{5, 3, 12, 25, 24, 54, 12, 42, 13, 49})
		);
	}

	@Test
	public void testWriteSplitWritesThatExceedPacketSize() {
		when(socket.getPacketPayloadSize()).thenReturn(10);

		byte[] data = new byte[]{5, 3, 12, 25, 24, 54, 12, 42, 13, 49, 5, 4, 42, 37};

		cut.write(data);

		ArgumentCaptor<ByteBuffer> bufferCaptor = ArgumentCaptor.forClass(ByteBuffer.class);
		verify(socket).send(bufferCaptor.capture());

		assertThat("Data over split packets must remain in order.", bufferCaptor.getValue().array(), equalTo(new byte[]{5, 3, 12, 25, 24, 54, 12, 42, 13, 49}));
		// The rest of the data should remain in the buffer.
	}

	@Test
	public void testWriteOnFlush() {
		when(socket.getPacketPayloadSize()).thenReturn(10);

		cut.write(5);

		verify(socket, never()).send(any());
		cut.flush();
		// Flushing twice should not cause a second send invocation.
		cut.flush();
		verify(socket, times(1)).send(any());
	}

}
