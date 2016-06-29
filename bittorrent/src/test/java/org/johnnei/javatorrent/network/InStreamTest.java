package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.time.Duration;

import org.johnnei.javatorrent.internal.utils.CheckedRunnable;
import org.johnnei.javatorrent.internal.utils.CheckedSupplier;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.powermock.reflect.Whitebox;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests {@link InStream}
 */
public class InStreamTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testRead() {
		InStream inStream = new InStream(new byte[]{
				1,
				23,
				0x01, 0x23,
				0x01, 0x23, 0x45, 0x67,
				0x01, 0x23, 0x45, 0x67, (byte) 0x89, (byte) 0xAB, (byte) 0xCD, (byte) 0xEF,
				(byte) 0xFF,
				(byte) 0xFF, (byte) 0xFF,
				0x01, 0x02, 0x03, 0x04,
				0x01, 0x02,
				0x01, 0x02, 0x03, 0x04,
				0x00, 0x4A,
				104, 101, 108, 108, 111, 32, 119, 111, 114, 108, 100
		});

		byte[] bufferReadFully = new byte[4];
		byte[] bufferReadFullyOffset = new byte[]{0x01, 0x02, 0x03, 0x04};
		byte[] expectedBytesReadFully = new byte[]{0x01, 0x02, 0x03, 0x04};
		byte[] expectedBytesReadFullyOffset = new byte[]{0x01, 0x01, 0x02, 0x04};
		byte[] expectedBytesReadFullyInt = new byte[]{0x01, 0x02, 0x03, 0x04};

		assertTrue("Available bytes is <= 0", inStream.available() > 0);

		assertTrue("readBoolean returned wrong value", inStream.readBoolean());
		assertEquals("readByte returned wrong value", 23, inStream.readByte());
		assertEquals("readShort returned wrong value", 0x123, inStream.readShort());
		assertEquals("readInt returned wrong value", 0x1234567, inStream.readInt());
		assertEquals("readLong returned wrong value", 0x123456789ABCDEFL, inStream.readLong());
		assertEquals("readUnsignedByte returned wrong value", 0xFF, inStream.readUnsignedByte());
		assertEquals("readUnsignedShort returned wrong value", 0xFFFF, inStream.readUnsignedShort());

		inStream.readFully(bufferReadFully);
		inStream.readFully(bufferReadFullyOffset, 1, 2);

		assertArrayEquals("readFully(byte[]) returned wrong value", expectedBytesReadFully, bufferReadFully);
		assertArrayEquals("readFully(byte[], int, int) returned wrong value", expectedBytesReadFullyOffset, bufferReadFullyOffset);
		assertArrayEquals("readFully(int) returned wrong value", expectedBytesReadFullyInt, inStream.readFully(4));

		assertEquals("readChar returned wrong value", 'J', inStream.readChar());
		assertEquals("readString returned wrong value", "hello world", inStream.readString(11));
		assertEquals("All data should have been read", 0, inStream.available());
	}

	@Test
	public void testSkipBytes() {
		InStream inStream = new InStream(new byte[] { 0x0, 0x0, 0x0 });

		assertEquals("Incorrect starting size", 3, inStream.available());
		inStream.skipBytes(2);
		assertEquals("Incorrect ending size", 1, inStream.available());
	}

	@Test
	public void testMoveBack() {
		InStream inStream = new InStream(new byte[] { 0x1, 0x2 });

		assertEquals("Incorrect starting size", 2, inStream.available());
		assertEquals("Incorrect byte value", 1, inStream.readByte());
		assertEquals("Incorrect available, should have read only 1 byte at this point.", 1, inStream.available());
		inStream.moveBack(1);
		assertEquals("Incorrect available, should have moved back to beginning of stream", 2, inStream.available());
		assertEquals("Incorrect byte value", 1, inStream.readByte());
		assertEquals("Incorrect byte value", 2, inStream.readByte());
	}

	@Test
	public void testMark() {
		InStream inStream = new InStream(new byte[] { 0x0, 0x0, 0x0, 0x0 });
		inStream.mark();
		inStream.readShort();
		assertEquals("Incorrect available value after reading short with mark", 2, inStream.available());
		inStream.resetToMark();
		assertEquals("Incorrect available value after returning to mark", 4, inStream.available());
	}

	@Test
	public void testDuration() {
		InStream inStream = new InStream(new byte[] {}, Duration.ZERO);
		assertEquals("Incorrect duration", Duration.ZERO, inStream.getReadDuration().get());
		inStream = new InStream(new byte[] { 0x0, 0x0 }, 1, 1);
		assertFalse("Incorrect duration", inStream.getReadDuration().isPresent());
	}

	@Test
	public void testExceptionDoUncheckedSupplier() throws Exception {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("IO Exception on in-memory byte array");

		CheckedSupplier<Integer, IOException> runnable = () -> { throw new IOException("Test exception path"); };
		InStream cut = new InStream(new byte[0]);

		Whitebox.invokeMethod(cut, "doUnchecked", runnable);
	}

	@Test
	public void testExceptionDoUncheckedRunnable() throws Exception {
		thrown.expect(RuntimeException.class);
		thrown.expectMessage("IO Exception on in-memory byte array");

		CheckedRunnable<IOException> runnable = () -> { throw new IOException("Test exception path"); };
		InStream cut = new InStream(new byte[0]);

		Whitebox.invokeMethod(cut, "doUnchecked", runnable);
	}

}