package org.johnnei.javatorrent.internal.utp.protocol;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.johnnei.javatorrent.internal.utp.protocol.payload.DataPayload;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Tests {@link UtpInputStream}
 */
public class UtpInputStreamTest {

	@Rule
	public Timeout timeout = new Timeout(5, TimeUnit.SECONDS);

	private UtpInputStream cut = new UtpInputStream((short) 0);

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

}