package org.johnnei.javatorrent.network;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.internal.utils.CheckedRunnable;
import org.johnnei.javatorrent.internal.utils.CheckedSupplier;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests {@link InStream}
 */
public class InStreamTest {

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

		assertTrue(inStream.available() > 0, "There should be data in the stream.");

		assertTrue(inStream.readBoolean(), "readBoolean returned wrong value");
		Assertions.assertEquals(23, inStream.readByte(), "readByte returned wrong value");
		Assertions.assertEquals(0x123, inStream.readShort(), "readShort returned wrong value");
		Assertions.assertEquals(0x1234567, inStream.readInt(), "readInt returned wrong value");
		Assertions.assertEquals(0x123456789ABCDEFL, inStream.readLong(), "readLong returned wrong value");
		Assertions.assertEquals(0xFF, inStream.readUnsignedByte(), "readUnsignedByte returned wrong value");
		Assertions.assertEquals(0xFFFF, inStream.readUnsignedShort(), "readUnsignedShort returned wrong value");

		inStream.readFully(bufferReadFully);
		inStream.readFully(bufferReadFullyOffset, 1, 2);

		assertArrayEquals(expectedBytesReadFully, bufferReadFully, "readFully(byte[]) returned wrong value");
		assertArrayEquals(expectedBytesReadFullyOffset, bufferReadFullyOffset, "readFully(byte[], int, int) returned wrong value");
		assertArrayEquals(expectedBytesReadFullyInt, inStream.readFully(4), "readFully(int) returned wrong value");

		Assertions.assertEquals('J', inStream.readChar(), "readChar returned wrong value");
		Assertions.assertEquals("hello world", inStream.readString(11), "readString returned wrong value");
		Assertions.assertEquals(0, inStream.available(), "All data should have been read");
	}

	@Test
	public void testSkipBytes() {
		InStream inStream = new InStream(new byte[] { 0x0, 0x0, 0x0 });

		Assertions.assertEquals(3, inStream.available(), "Incorrect starting size");
		inStream.skipBytes(2);
		Assertions.assertEquals(1, inStream.available(), "Incorrect ending size");
	}

	@Test
	public void testMoveBack() {
		InStream inStream = new InStream(new byte[] { 0x1, 0x2 });

		Assertions.assertEquals(2, inStream.available(), "Incorrect starting size");
		Assertions.assertEquals(1, inStream.readByte(), "Incorrect byte value");
		Assertions.assertEquals(1, inStream.available(), "Incorrect available, should have read only 1 byte at this point.");
		inStream.moveBack(1);
		Assertions.assertEquals(2, inStream.available(), "Incorrect available, should have moved back to beginning of stream");
		Assertions.assertEquals(1, inStream.readByte(), "Incorrect byte value");
		Assertions.assertEquals(2, inStream.readByte(), "Incorrect byte value");
	}

	@Test
	public void testMark() {
		InStream inStream = new InStream(new byte[] { 0x0, 0x0, 0x0, 0x0 });
		inStream.mark();
		inStream.readShort();
		Assertions.assertEquals(2, inStream.available(), "Incorrect available value after reading short with mark");
		inStream.resetToMark();
		Assertions.assertEquals(4, inStream.available(), "Incorrect available value after returning to mark");
	}

	@Test
	public void testDuration() {
		InStream inStream = new InStream(new byte[] {}, Duration.ZERO);
		Assertions.assertEquals(Duration.ZERO, inStream.getReadDuration().get(), "Incorrect duration");
		inStream = new InStream(new byte[] { 0x0, 0x0 }, 1, 1);
		assertFalse(inStream.getReadDuration().isPresent(), "Incorrect duration");
	}

	@Test
	public void testExceptionDoUncheckedSupplier() throws Exception {
		CheckedSupplier<Integer, IOException> runnable = () -> { throw new IOException("Test exception path"); };
		InStream cut = new InStream(new byte[0]);

		Exception e = assertThrows(RuntimeException.class, () -> Whitebox.invokeMethod(cut, "doUnchecked", runnable));
		assertThat(e.getMessage(), containsString("IO Exception on in-memory byte array"));
	}

	@Test
	public void testExceptionDoUncheckedRunnable() throws Exception {
		CheckedRunnable<IOException> runnable = () -> { throw new IOException("Test exception path"); };
		InStream cut = new InStream(new byte[0]);

		Exception e = assertThrows(RuntimeException.class, () -> Whitebox.invokeMethod(cut, "doUnchecked", runnable));
		assertThat(e.getMessage(), containsString("IO Exception on in-memory byte array"));
	}

}
