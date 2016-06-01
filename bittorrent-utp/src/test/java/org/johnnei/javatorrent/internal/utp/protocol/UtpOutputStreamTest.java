package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;
import java.util.List;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.payload.DataPayload;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertArrayEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpOutputStream}
 */
@RunWith(MockitoJUnitRunner.class)
public class UtpOutputStreamTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Mock
	private UtpSocketImpl socketMock;

	@InjectMocks
	private UtpOutputStream cut;

	@Before
	public void setUp() {
		when(socketMock.getPacketSize()).thenReturn(5);
	}

	@Test
	public void testExceptionOnClosingSocket() throws IOException {
		thrown.expect(IOException.class);
		thrown.expectMessage("closed");

		when(socketMock.getConnectionState()).thenReturn(ConnectionState.DISCONNECTING);
		cut.write(5);
	}

	@Test
	public void testExceptionOnClosedSocket() throws IOException {
		thrown.expect(IOException.class);
		thrown.expectMessage("closed");

		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CLOSED);
		cut.write(5);
	}

	@Test
	public void testBufferedWrite() throws IOException {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

		cut.write(5);
		cut.write(4);
		cut.write(3);
		cut.write(2);
		cut.write(1);

		ArgumentCaptor<DataPayload> payloadArgumentCaptor = ArgumentCaptor.forClass(DataPayload.class);
		verify(socketMock).send(payloadArgumentCaptor.capture());

		byte[] output = payloadArgumentCaptor.getValue().getData();
		assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, output);
	}

	@Test
	public void testBufferedWriteArray() throws IOException {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

		cut.write(new byte[] { 5, 4, 3, 2, 1 }, 0, 2);
		cut.write(new byte[] { 5, 4, 3, 2, 1 }, 2, 3);

		ArgumentCaptor<DataPayload> payloadArgumentCaptor = ArgumentCaptor.forClass(DataPayload.class);
		verify(socketMock).send(payloadArgumentCaptor.capture());

		byte[] output = payloadArgumentCaptor.getValue().getData();
		assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, output);
	}

	@Test
	public void testBufferedWriteArrayMultiplePackets() throws IOException {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

		cut.write(new byte[] { 5, 4, 3, 2, 1, 1, 2, 3, 4, 5 });

		ArgumentCaptor<DataPayload> payloadArgumentCaptor = ArgumentCaptor.forClass(DataPayload.class);
		verify(socketMock, times(2)).send(payloadArgumentCaptor.capture());

		List<DataPayload> payloads = payloadArgumentCaptor.getAllValues();

		byte[] output = payloads.get(0).getData();
		assertArrayEquals("Incorrect output on first packet", new byte[] { 5, 4, 3, 2, 1 }, output);

		output = payloads.get(1).getData();
		assertArrayEquals("Incorrect output on second packet", new byte[] { 1, 2, 3, 4, 5 }, output);
	}

	@Test
	public void testBufferedWriteMixedWriteArray() throws IOException {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

		cut.write(new byte[] { 5, 4, 3, 2, 1 }, 0, 2);
		cut.write(3);
		cut.write(new byte[] { 5, 4, 3, 2, 1 }, 3, 2);

		ArgumentCaptor<DataPayload> payloadArgumentCaptor = ArgumentCaptor.forClass(DataPayload.class);
		verify(socketMock).send(payloadArgumentCaptor.capture());

		byte[] output = payloadArgumentCaptor.getValue().getData();
		assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, output);
	}

	@Test
	public void testBufferedWriteArrayMixedWrite() throws IOException {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

		cut.write(new byte[] { 5, 4, 3, 2, 1 }, 0, 2);
		cut.write(new byte[] { 5, 4, 3, 2, 1 }, 2, 2);
		cut.write(1);

		ArgumentCaptor<DataPayload> payloadArgumentCaptor = ArgumentCaptor.forClass(DataPayload.class);
		verify(socketMock).send(payloadArgumentCaptor.capture());

		byte[] output = payloadArgumentCaptor.getValue().getData();
		assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, output);
	}

	@Test
	public void testBufferingOnScalingPacketSize() throws IOException {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

		cut.write(new byte[] { 5, 4, 3, 2, 1 }, 0, 2);
		when(socketMock.getPacketSize()).thenReturn(10);
		cut.write(new byte[] { 5, 4, 3, 2, 1, 5, 4, 3, 2, 1 }, 2, 8);
		cut.write(1);

		ArgumentCaptor<DataPayload> payloadArgumentCaptor = ArgumentCaptor.forClass(DataPayload.class);
		verify(socketMock).send(payloadArgumentCaptor.capture());

		byte[] output = payloadArgumentCaptor.getValue().getData();
		assertArrayEquals(new byte[] { 5, 4, 3, 2, 1, 5, 4, 3, 2, 1 }, output);
	}

	@Test
	public void testFlushOnNoWrittenBytes() throws IOException {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

		cut.flush();

		verifyNoMoreInteractions(socketMock);
	}

	@Test
	public void testFlush() throws IOException {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

		cut.write(new byte[] { 5, 4 });
		cut.flush();
		cut.write(1);
		cut.write(2);
		cut.flush();

		ArgumentCaptor<DataPayload> payloadArgumentCaptor = ArgumentCaptor.forClass(DataPayload.class);
		verify(socketMock, times(2)).send(payloadArgumentCaptor.capture());

		assertArrayEquals(new byte[] { 5, 4 }, payloadArgumentCaptor.getAllValues().get(0).getData());
		assertArrayEquals(new byte[] { 1, 2 }, payloadArgumentCaptor.getAllValues().get(1).getData());
	}

}