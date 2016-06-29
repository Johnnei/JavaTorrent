package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.johnnei.javatorrent.internal.network.socket.UtpSocketImpl;
import org.johnnei.javatorrent.internal.utp.protocol.payload.DataPayload;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.Timeout;
import org.powermock.reflect.Whitebox;

import static com.jayway.awaitility.Awaitility.await;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link UtpInputStream}
 */
public class UtpInputStreamTest {

	@Rule
	public Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private UtpSocketImpl socketMock = mock(UtpSocketImpl.class);

	private UtpInputStream cut = new UtpInputStream(socketMock, (short) 0);

	@Test
	public void testAvailable() {
		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));
		assertEquals("Incorrect amount available", 5, cut.available());
	}

	@Test
	public void testAvailableMultiplePackets() {
		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));
		cut.addToBuffer((short) 1, new DataPayload(new byte[] { 1, 2, 3, 4, 5 }));
		assertEquals("Incorrect amount available", 10, cut.available());

	}

	@Test
	public void testAvailableDisorderedPackets() {
		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));
		assertEquals("Incorrect amount available", 5, cut.available());
		cut.addToBuffer((short) 2, new DataPayload(new byte[] { 1, 2, 3, 4, 5 }));
		assertEquals("Incorrect amount available", 5, cut.available());
		cut.addToBuffer((short) 1, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));
		assertEquals("Incorrect amount available", 15, cut.available());
	}

	@Test
	public void testRead() throws IOException {
		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));
		assertEquals("Incorrect amount available", 5, cut.available());
		assertEquals(5, cut.read());
		assertEquals(4, cut.read());
		assertEquals(3, cut.read());
		assertEquals(2, cut.read());
		assertEquals(1, cut.read());
		assertEquals("Incorrect amount available", 0, cut.available());
	}

	@Test
	public void testReadArray() throws IOException {
		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));

		byte[] partOne = new byte[3];
		byte[] partTwo = new byte[3];

		assertEquals("Incorrect amount available", 5, cut.available());
		assertEquals("Incorrect amount of bytes read.", 3, cut.read(partOne));
		assertEquals("Incorrect amount of bytes read.", 2, cut.read(partTwo, 0, 2));
		assertArrayEquals("First buffer is incorrect", new byte[] { 5, 4, 3 }, partOne);
		assertArrayEquals("Second buffer is incorrect", new byte[] { 2, 1, 0 }, partTwo);
		assertEquals("Incorrect amount available", 0, cut.available());
	}

	@Test
	public void testReadArrayOverRead() throws IOException {
		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));

		byte[] partOne = new byte[3];
		byte[] partTwo = new byte[3];

		assertEquals("Incorrect amount available", 5, cut.available());
		assertEquals("Incorrect amount of bytes read.", 3, cut.read(partOne));
		assertEquals("Incorrect amount of bytes read.", 2, cut.read(partTwo, 0, 3));
		assertArrayEquals("First buffer is incorrect", new byte[] { 5, 4, 3 }, partOne);
		assertArrayEquals("Second buffer is incorrect", new byte[] { 2, 1, 0 }, partTwo);
		assertEquals("Incorrect amount available", 0, cut.available());
	}

	@Test
	public void testReadArrayOverReadOffset() throws IOException {
		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));

		byte[] partOne = new byte[6];

		assertEquals("Incorrect amount available", 5, cut.available());
		assertEquals("Incorrect amount of bytes read.", 3, cut.read(partOne, 0, 3));
		assertEquals("Incorrect amount of bytes read.", 2, cut.read(partOne, 3, 3));
		assertArrayEquals("Buffered data is incorrect", new byte[] { 5, 4, 3, 2, 1, 0 }, partOne);
		assertEquals("Incorrect amount available", 0, cut.available());
	}

	@Test
	public void testReadFromMultiplePackets() throws IOException {
		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));
		cut.addToBuffer((short) 1, new DataPayload(new byte[] { 1, 2, 3, 4, 5 }));
		byte[] expectedOutput = new byte[] {
				5, 4, 3, 2, 1,
				1, 2, 3, 4, 5
		};

		byte[] output = new byte[10];
		assertEquals("Incorrect amount of bytes read", 10, cut.read(output));
		assertArrayEquals("Incorrect output", expectedOutput, output);
		assertEquals("Incorrect amount available", 0, cut.available());
	}

	@Test
	public void testReadFromMultipleDisorderedPackets() throws IOException {
		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));
		cut.addToBuffer((short) 2, new DataPayload(new byte[] { 5, 4, 3, 2, 1 }));
		cut.addToBuffer((short) 1, new DataPayload(new byte[] { 1, 2, 3, 4, 5 }));
		byte[] expectedOutput = new byte[] {
				5, 4, 3, 2, 1,
				1, 2, 3, 4, 5,
				5, 4, 3, 2, 1
		};

		byte[] output = new byte[15];
		assertEquals("Incorrect amount of bytes read", 15, cut.read(output));
		assertArrayEquals("Incorrect output", expectedOutput, output);
		assertEquals("Incorrect amount available", 0, cut.available());
	}

	/**
	 * Scenario: Reading two blocks, one of which is before the FIN state sequence number (and thus should be waited for) and the second being EOF.
	 * @throws Exception
	 */
	@Test
	public void testReadBlockOutOfOrderEOF() throws Exception {
		thrown.expect(EOFException.class);
		thrown.expectMessage("shutdown");
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CLOSED);

		// Cause EOF
		cut.read();
	}

	@Test
	public void testReadBlockEOF() throws Exception {
		thrown.expect(EOFException.class);
		thrown.expectMessage("shutdown");
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.DISCONNECTING);
		when(socketMock.getEndOfStreamSequenceNumber()).thenReturn((short) 1);

		ReentrantLock cutLock = Whitebox.getInternalState(cut, "notifyLock");
		Condition cutCondition = Whitebox.getInternalState(cut, "onPacketArrived");

		FutureTask<Integer> future = new FutureTask<>(() -> cut.read());

		new Thread(future).start();

		await().atMost(1, TimeUnit.SECONDS).until(() -> {
			cutLock.lock();
			try {
				cutLock.hasWaiters(cutCondition);
			} finally {
				cutLock.unlock();
			}
		});

		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 42 }));

		assertEquals("Incorrect byte has been returned from read", 42, (int) future.get(5, TimeUnit.SECONDS));

		// Cause EOF
		cut.read();
	}

	@Test
	public void testReadBlockUntilAvailable() throws Exception {
		when(socketMock.getConnectionState()).thenReturn(ConnectionState.CONNECTED);

		ReentrantLock cutLock = Whitebox.getInternalState(cut, "notifyLock");
		Condition cutCondition = Whitebox.getInternalState(cut, "onPacketArrived");

		FutureTask<Integer> future = new FutureTask<>(() -> cut.read());

		new Thread(future).start();

		await().atMost(1, TimeUnit.SECONDS).until(() -> {
			cutLock.lock();
			try {
				cutLock.hasWaiters(cutCondition);
			} finally {
				cutLock.unlock();
			}
		});

		cut.addToBuffer((short) 0, new DataPayload(new byte[] { 42 }));

		assertEquals("Incorrect byte has been returned from read", 42, (int) future.get(5, TimeUnit.SECONDS));
	}

}