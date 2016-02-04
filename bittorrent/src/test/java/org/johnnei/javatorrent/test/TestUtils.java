package org.johnnei.javatorrent.test;

public class TestUtils {

	private TestUtils() {
		/* No constructors for you! */
	}

	public static void copySection(byte[] from, byte[] to, int targetStart) {
		copySection(from, to, targetStart, 0, from.length);
	}

	public static void copySection(byte[] from, byte[] to, int targetStart, int start, int count) {
		for (int i = 0; i < count; i++) {
			to[targetStart + i] = from[start + i];
		}
	}

}
