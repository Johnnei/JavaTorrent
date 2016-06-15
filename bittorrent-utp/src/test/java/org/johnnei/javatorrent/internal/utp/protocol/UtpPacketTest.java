package org.johnnei.javatorrent.internal.utp.protocol;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utp.protocol.payload.IPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.UtpPayloadFactory;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpPacket}
 */
@RunWith(MockitoJUnitRunner.class)
public class UtpPacketTest {

	@Mock
	private PrecisionTimer precisionTimerMock;

	@Mock
	private IPayload payloadMock;

	@InjectMocks
	private UtpPacket cut = new UtpPacket();

	@Test
	public void testReadAndProcess() throws Exception {
		UtpPayloadFactory factoryMock = mock(UtpPayloadFactory.class);
		IPayload payloadMock = mock(IPayload.class);
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		InStream inStream = new InStream(new byte[] {
				// Type and version
				0x21,
				// Extension
				0x00,
				// Connection Id
				0x12, 0x34,
				// Timestamp
				0x12, 0x34, 0x56, 0x78,
				// Timestamp difference
				(byte) 0x87, 0x65, 0x43, 0x21,
				// Window size
				0x12, 0x34, 0x43, 0x21,
				// Sequence number
				0x43, 0x21,
				// Acknowledge number
				0x12, 0x21
		});

		when(factoryMock.createPayloadFromType(2)).thenReturn(payloadMock);

		cut.read(inStream, factoryMock);
		cut.processPayload(socketMock);

		verify(payloadMock).process(same(cut), same(socketMock));

		assertEquals("Incorrect version has been read.", 1, cut.getVersion());
		assertEquals("Incorrect type has been read.", 2, cut.getType());
		assertFalse("Packet did not have any extensions in header.", cut.hasExtensions());
		assertEquals("Incorrect connection id has been read.", 0x1234, cut.getConnectionId());
		assertEquals("Incorrect timestamp has been read.", 0x12345678, cut.getTimestampMicroseconds());
		assertEquals("Incorrect timestamp difference has been read.", 0x87654321, cut.getTimestampDifferenceMicroseconds());
		assertEquals("Incorrect window size has been read.", 0x12344321, cut.getWindowSize());
		assertEquals("Incorrect sequence number has been read.", 0x4321, cut.getSequenceNumber());
		assertEquals("Incorrect acknowledge number has been read.", 0x1221, cut.getAcknowledgeNumber());
	}

	@Test
	public void testReadIncorrectVersion() throws Exception {
		UtpPayloadFactory factoryMock = mock(UtpPayloadFactory.class);
		InStream inStream = new InStream(new byte[] { 0x02 });

		cut.read(inStream, factoryMock);

		assertEquals("Incorrect version has been read.", 2, cut.getVersion());
	}

	@Test
	public void testUpdateSentTime() {
		when(precisionTimerMock.getCurrentMicros()).thenReturn(42);

		cut.updateSentTime();

		assertEquals("Timestamp was not correctly copied from the precision timer", 42, cut.getSentTime());
	}

	@Test
	public void testPacketSize() {
		when(payloadMock.getSize()).thenReturn(5);

		assertEquals("Packet size + payload size must be added together.", 25, cut.getPacketSize());
	}

	@Test
	public void testWritePacket() {
		OutStream outStream = new OutStream();
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		PrecisionTimer timerMock = mock(PrecisionTimer.class);

		when(socketMock.nextSequenceNumber()).thenReturn((short) 1);
		when(socketMock.getSendingConnectionId()).thenReturn((short) 0x1235);
		when(socketMock.getMeasuredDelay()).thenReturn(0);
		when(socketMock.getWindowSize()).thenReturn(0x12344321);
		when(socketMock.getAcknowledgeNumber()).thenReturn((short) 0);
		when(payloadMock.getType()).thenReturn((byte) UtpProtocol.ST_DATA);
		when(timerMock.getCurrentMicros()).thenReturn(0x000DAC00);

		UtpPacket cut = new UtpPacket(socketMock, payloadMock);
		Whitebox.setInternalState(cut, PrecisionTimer.class, timerMock);
		cut.write(socketMock, outStream);

		byte[] expectedOutput = new byte[] {
				// Type and version
				0x01,
				// Extension
				0x00,
				// Connection Id
				0x12, 0x35,
				// Timestamp
				0x00, 0x0D, (byte) 0xAC, 0x00,
				// Timestamp difference
				0x00, 0x00, 0x00, 0x00,
				// Window size
				0x12, 0x34, 0x43, 0x21,
				// Sequence number
				0x00, 0x01,
				// Acknowledge number
				0x00, 0x00
		};

		assertArrayEquals("Incorrect output data", expectedOutput, outStream.toByteArray());
	}

	@Test
	public void testWriteSynPacket() {
		OutStream outStream = new OutStream();
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		IPayload payloadMock = mock(IPayload.class);
		PrecisionTimer timerMock = mock(PrecisionTimer.class);

		when(socketMock.nextSequenceNumber()).thenReturn((short) 1);
		when(socketMock.getReceivingConnectionId()).thenReturn((short) 0x1234);
		when(socketMock.getMeasuredDelay()).thenReturn(0);
		when(socketMock.getWindowSize()).thenReturn(0x12344321);
		when(socketMock.getAcknowledgeNumber()).thenReturn((short) 0);
		when(payloadMock.getType()).thenReturn((byte) UtpProtocol.ST_SYN);
		when(timerMock.getCurrentMicros()).thenReturn(0x000DAC00);

		UtpPacket cut = new UtpPacket(socketMock, payloadMock);
		Whitebox.setInternalState(cut, PrecisionTimer.class, timerMock);
		cut.write(socketMock, outStream);

		byte[] expectedOutput = new byte[] {
				// Type and version
				0x41,
				// Extension
				0x00,
				// Connection Id
				0x12, 0x34,
				// Timestamp
				0x00, 0x0D, (byte) 0xAC, 0x00,
				// Timestamp difference
				0x00, 0x00, 0x00, 0x00,
				// Window size
				0x12, 0x34, 0x43, 0x21,
				// Sequence number
				0x00, 0x01,
				// Acknowledge number
				0x00, 0x00
		};

		assertArrayEquals("Incorrect output data", expectedOutput, outStream.toByteArray());
	}

	@Test
	public void testToString() {
		UtpSocketImpl socketMock = mock(UtpSocketImpl.class);
		IPayload payloadMock = mock(IPayload.class);

		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTING);
		when(payloadMock.getType()).thenReturn((byte) 3);
		when(socketMock.nextSequenceNumber()).thenReturn((short) 5);
		when(socketMock.getSendingConnectionId()).thenReturn((short) 7);

		UtpPacket cut = new UtpPacket(socketMock, payloadMock);
		assertTrue("Incorrect toString start", cut.toString().startsWith("UtpPacket["));
	}

}