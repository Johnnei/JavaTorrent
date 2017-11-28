package org.johnnei.javatorrent.network;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.powermock.reflect.Whitebox;

import org.johnnei.javatorrent.internal.utils.CheckedRunnable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


/**
 * Tests {@link OutStream}
 */
public class OutStreamTest {

	private OutStream cut;

	@BeforeEach
	public void setUp() {
		cut = new OutStream();
	}

	@Test
	public void testWriteByteArray() throws Exception {
		byte[] expectedOutput = new byte[] { 1, 2, 3, 4 };

		cut.write(expectedOutput);

		assertEquals(4, cut.size(), "Incorrect amount of bytes written");
		assertArrayEquals(expectedOutput, cut.toByteArray(), "Incorrect written bytes");
	}

	@Test
	public void testWriteByteArrayOffset() throws Exception {
		byte[] expectedOutput = new byte[] { 2, 3 };
		byte[] input = new byte[] { 1, 2, 3, 4 };

		cut.write(input, 1, 2);

		assertEquals(2, cut.size(), "Incorrect amount of bytes written");
		assertArrayEquals(expectedOutput, cut.toByteArray(), "Incorrect written bytes");
	}

	@Test
	public void testWriteBoolean() throws Exception {
		byte[] expectedOutput = new byte[] { 0, 1 };

		cut.writeBoolean(false);
		cut.writeBoolean(true);

		assertEquals(2, cut.size(), "Incorrect amount of bytes written");
		assertArrayEquals(expectedOutput, cut.toByteArray(), "Incorrect written bytes");
	}

	@Test
	public void testWriteByte() throws Exception {
		byte[] expectedOutput = new byte[] { 45 };

		cut.writeByte(45);

		assertEquals(1, cut.size(), "Incorrect amount of bytes written");
		assertArrayEquals(expectedOutput, cut.toByteArray(), "Incorrect written bytes");
	}

	@Test
	public void testWriteInt() throws Exception {
		byte[] expectedOutput = new byte[] { 0x5F, (byte) 0xC2, 0x2F, (byte) 0xF7 };

		cut.writeInt(1606561783);

		assertEquals(4, cut.size(), "Incorrect amount of bytes written");
		assertArrayEquals(expectedOutput, cut.toByteArray(), "Incorrect written bytes");
	}

	@Test
	public void testWriteLong() throws Exception {
		byte[] expectedOutput = new byte[] { 0x2C, (byte) 0x97, 0x4E, (byte) 0xDC, 0x5B, (byte) 0xEC, 0x76, 0x58 };

		cut.writeLong(3213123567494133336L);

		assertEquals(8, cut.size(), "Incorrect amount of bytes written");
		assertArrayEquals(expectedOutput, cut.toByteArray(), "Incorrect written bytes");
	}

	@Test
	public void testWriteShort() throws Exception {
		byte[] expectedOutput = new byte[] { 0x7D, (byte) 0x90 };

		cut.writeShort(32144);

		assertEquals(2, cut.size(), "Incorrect amount of bytes written");
		assertArrayEquals(expectedOutput, cut.toByteArray(), "Incorrect written bytes");
	}

	@Test
	public void testWriteString() throws Exception {
		byte[] expectedOutput = new byte[] {
				0x42, 0x69, 0x74, 0x54, 0x6F, 0x72, 0x72, 0x65, 0x6E, 0x74, 0x20,
				0x70, 0x72, 0x6F, 0x74, 0x6F, 0x63, 0x6F, 0x6C
		};

		cut.writeString("BitTorrent protocol");

		assertEquals(0x13, cut.size(), "Incorrect amount of bytes written");
		assertArrayEquals(expectedOutput, cut.toByteArray(), "Incorrect written bytes");

	}

	@Test
	public void testExceptionWriteUnchecked() throws Exception {
		CheckedRunnable<IOException> runnable = () -> { throw new IOException("Test exception path"); };
		OutStream cut = new OutStream();

		RuntimeException exception = assertThrows(RuntimeException.class, () -> Whitebox.invokeMethod(cut, "writeUnchecked", runnable));
		assertThat(exception.getMessage(), containsString("IO Exception on in-memory byte array"));
	}
}
