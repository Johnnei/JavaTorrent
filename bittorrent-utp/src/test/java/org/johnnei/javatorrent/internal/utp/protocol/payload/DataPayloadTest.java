package org.johnnei.javatorrent.internal.utp.protocol.payload;

import java.io.IOException;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.UtpInputStream;
import org.johnnei.javatorrent.internal.utp.protocol.UtpPacket;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link DataPayload}
 */
public class DataPayloadTest {

	@Test
	public void testGetType() {
		assertEquals(0, new DataPayload().getType());
	}

	@Test
	public void testWrite() {
		OutStream outStream = new OutStream();

		DataPayload cut = new DataPayload(new byte[] { 5, 4, 3, 2, 1 });
		cut.write(outStream);

		assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, outStream.toByteArray());
		assertEquals("Incorrect size", 5, cut.getSize());
	}

	@Test
	public void testRead() {
		InStream inStream = new InStream(new byte[] { 5, 4, 3, 2, 1 });

		DataPayload cut = new DataPayload();
		cut.read(inStream);

		assertArrayEquals(new byte[] { 5, 4, 3, 2, 1 }, cut.getData());
	}

	@Test
	public void testProcess() throws IOException {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		UtpPacket packetMock = mock(UtpPacket.class);
		UtpInputStream inputStreamMock = mock(UtpInputStream.class);

		when(socketMock.getInputStream()).thenReturn(inputStreamMock);
		when(packetMock.getSequenceNumber()).thenReturn((short) 3);

		DataPayload cut = new DataPayload(new byte[] { 5, 4, 3, 2, 1 });
		cut.process(packetMock, socketMock);

		verify(inputStreamMock).addToBuffer(eq((short) 3), same(cut));
	}

	@Test
	public void testToString() {
		assertTrue("Incorrect toString start", new DataPayload().toString().startsWith("DataPayload["));
	}

}