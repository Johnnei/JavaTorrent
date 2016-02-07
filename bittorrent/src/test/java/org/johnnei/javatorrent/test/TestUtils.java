package org.johnnei.javatorrent.test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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

	/**
	 * Tests the 3 mandatory checks on the equals method:
	 * <ol>
	 *   <li>this == this -&gt; true</li>
	 *   <li>this == null -&gt; false</li>
	 *   <li>this not instance of type -&gt; false (Tested with comparing the integer 7)</li>
	 * </ul>
	 * @param o The object to test on
	 */
	public static void assertEqualsMethod(Object o) {
		assertTrue("Same object instance aren't equal", o.equals(o));
		assertFalse("Object equals with null", o.equals(null));
		assertFalse("Object matches with not castable type", o.equals(7));
	}

}
