package org.johnnei.javatorrent.network;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Helper methods which reduce boiler-plate code for {@link ByteBuffer}
 */
public final class ByteBufferUtils {

	private ByteBufferUtils() {
		// No instances for utility classes.
	}

	/**
	 * Puts the given string as UTF-8 into the buffer.
	 * @param buffer The buffer to append the string to.
	 * @param s The string to put.
	 */
	public static void putString(ByteBuffer buffer, String s) {
		buffer.put(s.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * Gets a UTF-8 string of the given length in bytes.
	 * @param buffer The buffer to get from.
	 * @param length The amount of bytes to consume.
	 * @return The read string.
	 */
	public static String getString(ByteBuffer buffer, int length) {
		byte[] str = new byte[length];
		buffer.get(str);
		return new String(str, StandardCharsets.UTF_8);
	}

	/**
	 * Gets the next <code>length</code> bytes as a byte array.
	 * @param buffer The buffer to get from.
	 * @param length The amount of bytes to consume.
	 * @return The read byte array.
	 */
	public static byte[] getBytes(ByteBuffer buffer, int length) {
		byte[] bytes = new byte[length];
		buffer.get(bytes);
		return bytes;
	}
}
