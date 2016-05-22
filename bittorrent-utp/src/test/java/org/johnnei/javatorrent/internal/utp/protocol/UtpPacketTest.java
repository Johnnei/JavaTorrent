package org.johnnei.javatorrent.internal.utp.protocol;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utils.PrecisionTimer;
import org.johnnei.javatorrent.internal.utp.protocol.payload.IPayload;
import org.johnnei.javatorrent.internal.utp.protocol.payload.UtpPayloadFactory;
import org.johnnei.javatorrent.network.InStream;
import org.johnnei.javatorrent.network.OutStream;

import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.notNull;
import static org.easymock.EasyMock.same;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link UtpPacket}
 */
public class UtpPacketTest extends EasyMockSupport {

	@Test
	public void testReadAndProcess() throws Exception {
		UtpPayloadFactory factoryMock = createMock(UtpPayloadFactory.class);
		IPayload payloadMock = createMock(IPayload.class);
		UtpSocketImpl socketMock = createMock(UtpSocketImpl.class);
		InStream inStream = new InStream(new byte[] {
				// Type and version
				0x01,
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

		expect(factoryMock.createPayloadFromType(0)).andReturn(payloadMock);
		payloadMock.read(notNull());

		UtpPacket cut = new UtpPacket();
		payloadMock.process(same(cut), same(socketMock));

		replayAll();

		cut.read(inStream, factoryMock);
		cut.processPayload(socketMock);

		verifyAll();

		assertEquals("Incorrect version has been read.", 1, cut.getVersion());
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
		UtpPayloadFactory factoryMock = createMock(UtpPayloadFactory.class);
		InStream inStream = new InStream(new byte[] { 0x02 });

		replayAll();

		UtpPacket cut = new UtpPacket();
		cut.read(inStream, factoryMock);

		verifyAll();

		assertEquals("Incorrect version has been read.", 2, cut.getVersion());
	}
	@Test
	public void testWritePacket() {
		OutStream outStream = new OutStream();
		UtpSocketImpl socketMock = createMock(UtpSocketImpl.class);
		IPayload payloadMock = createMock(IPayload.class);
		PrecisionTimer timerMock = createMock(PrecisionTimer.class);

		expect(socketMock.nextSequenceNumber()).andReturn((short) 1);
		expect(socketMock.getSendingConnectionId()).andReturn((short) 0x1235);
		expect(socketMock.getMeasuredDelay()).andReturn(0);
		expect(socketMock.getWindowSize()).andReturn(0x12344321);
		expect(socketMock.getAcknowledgeNumber()).andReturn((short) 0);
		expect(payloadMock.getType()).andReturn((byte) UtpProtocol.ST_DATA);
		expect(timerMock.getCurrentMicros()).andReturn(0x000DAC00);

		payloadMock.write(notNull());

		replayAll();

		UtpPacket cut = new UtpPacket(socketMock, payloadMock);
		Whitebox.setInternalState(cut, PrecisionTimer.class, timerMock);
		cut.write(socketMock, outStream);

		verifyAll();

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
		UtpSocketImpl socketMock = createMock(UtpSocketImpl.class);
		IPayload payloadMock = createMock(IPayload.class);
		PrecisionTimer timerMock = createMock(PrecisionTimer.class);

		expect(socketMock.nextSequenceNumber()).andReturn((short) 1);
		expect(socketMock.getReceivingConnectionId()).andReturn((short) 0x1234);
		expect(socketMock.getMeasuredDelay()).andReturn(0);
		expect(socketMock.getWindowSize()).andReturn(0x12344321);
		expect(socketMock.getAcknowledgeNumber()).andReturn((short) 0);
		expect(payloadMock.getType()).andReturn((byte) UtpProtocol.ST_SYN);
		expect(timerMock.getCurrentMicros()).andReturn(0x000DAC00);

		payloadMock.write(notNull());

		replayAll();

		UtpPacket cut = new UtpPacket(socketMock, payloadMock);
		Whitebox.setInternalState(cut, PrecisionTimer.class, timerMock);
		cut.write(socketMock, outStream);

		verifyAll();

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
		UtpSocketImpl socketMock = createMock(UtpSocketImpl.class);
		IPayload payloadMock = createMock(IPayload.class);

		expect(socketMock.getConnectionState()).andReturn(ConnectionState.CONNECTING);
		expect(payloadMock.getType()).andReturn((byte) 3);
		expect(socketMock.nextSequenceNumber()).andReturn((short) 5);
		expect(socketMock.getSendingConnectionId()).andReturn((short) 7);

		replayAll();

		UtpPacket cut = new UtpPacket(socketMock, payloadMock);
		assertTrue("Incorrect toString start", cut.toString().startsWith("UtpPacket["));
	}

}